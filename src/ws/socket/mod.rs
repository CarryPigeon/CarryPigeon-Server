use crate::dao::REDIS_POOL;
use crate::model::protocol::ws::request::WebSocketRequest;
use crate::model::protocol::ws::response::WebSocketResponse;
use crate::model::vo::account::user::{UserLoginResponseVo, UserLoginVo};
use crate::model::ws::CPSender;
use crate::service::account::user::{push_user_service, remove_user_service, user_login_service};
use crate::utils::id::generate_id;
use crate::ws::dispatcher::ws_dispatcher;
use base64::engine::general_purpose;
use base64::Engine;
use rbatis::rbatis_codegen::ops::AsProxy;
use redis::AsyncCommands;
use rocket::futures::StreamExt;
use rocket::get;
use rocket::serde::json::serde_json::json;
use rocket_ws::Message;
use std::cell::SyncUnsafeCell;
use std::collections::HashMap;
use std::sync::{Arc, OnceLock};
use tokio::sync::Mutex;

pub static MESSAGE_MAP: OnceLock<SyncUnsafeCell<HashMap<i64, bool>>> = OnceLock::new();

pub fn init_message_map() {
    let _ = MESSAGE_MAP.set(SyncUnsafeCell::new(HashMap::new()));
}

#[get("/connect?<username>&<password>")]
pub async fn websocket_service(
    ws: rocket_ws::WebSocket,
    username: &str,
    password: &str,
) -> rocket_ws::Channel<'static> {
    tracing::info!("{} try to login", username);
    let info = UserLoginVo {
        username: username.to_string(),
        password: password.to_string(),
    };
    let user = user_login_service(info.to_dto()).await;
    // 创建到client的websocket连接
    ws.channel(move |stream| {
        Box::pin(async move {
            // 对stream进行读写分离
            let (sender, mut receiver) = stream.split();
            // 对sender进行封装
            let sender = Arc::new(Mutex::new(CPSender::new(sender)));
            match user {
                None => {
                    let _ = sender
                        .lock()
                        .await
                        .send_ws_data(WebSocketResponse::error(json!("login error")))
                        .await;
                    return Ok(());
                }
                Some(user) => {
                    // 获取用户id
                    let id = user.id.unwrap();
                    // 生成用户token
                    let token = general_purpose::STANDARD.encode(generate_id().as_binary());
                    // 将token返回
                    let _ = sender
                        .lock()
                        .await
                        .send_ws_data(WebSocketResponse::send(
                            json!(UserLoginResponseVo {
                                token: token.clone()
                            }),
                            "/token".to_string(),
                        ))
                        .await;
                    push_user_service(user, Arc::clone(&sender), token).await;
                    let mut shut_flag = false;
                    while let Some(message) = receiver.next().await {
                        // 接收信息
                        if unsafe {
                            *MESSAGE_MAP
                                .get()
                                .unwrap()
                                .get()
                                .as_mut()
                                .unwrap()
                                .get(&id)
                                .unwrap()
                        } {
                            unsafe {
                                let temp = REDIS_POOL.get().unwrap().get().as_mut().unwrap();
                                let temp_pool: Vec<String> = temp.get(id).await.unwrap();
                                if !temp_pool.is_empty() {
                                    for i in temp_pool {
                                        let _ = sender.lock().await.send_json(i).await;
                                    }
                                }
                                MESSAGE_MAP
                                    .get()
                                    .unwrap()
                                    .get()
                                    .as_mut()
                                    .unwrap()
                                    .insert(id, false);
                            }
                        }

                        // 处理发送信息
                        match message {
                            Err(error) => {
                                tracing::error!("{}", format!("websocket error,msg:{:?}", error));
                                shut_flag = true;
                            }
                            Ok(message) => {
                                match message {
                                    Message::Text(text) => {
                                        // 进行路径分配处理
                                        let data = WebSocketRequest::new(&text);
                                        match data {
                                            Ok(request) => {
                                                let mes_id = &request.request_id.clone();
                                                let mut result = ws_dispatcher(request).await;
                                                // 对result进行标识
                                                result.id = *mes_id;
                                                let _ =
                                                    sender.lock().await.send_ws_data(result).await;
                                            }
                                            Err(error) => {
                                                tracing::error!(
                                                    "{}",
                                                    format!(
                                                        "websocket error json structure,msg:{:?}",
                                                        error
                                                    )
                                                );
                                                shut_flag = true;
                                            }
                                        }
                                    }
                                    Message::Binary(_binary) => {}
                                    Message::Ping(_) => {
                                        let _ = sender.lock().await.send_pong().await;
                                    }
                                    Message::Pong(_) => {
                                        // 暂不接受Pong
                                        //shut_flag = true;
                                    }
                                    Message::Close(_) => {
                                        shut_flag = true;
                                    }
                                    Message::Frame(_frame) => {}
                                }
                            }
                        }
                        // 检查flag
                        if shut_flag {
                            // 执行清理工作
                            remove_user_service(id).await;
                            break;
                        }
                    }
                }
            }
            Ok(())
        })
    })
}
