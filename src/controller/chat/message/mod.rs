use crate::{
    dao::{MYSQL_POOL, REDIS_POOL, message::Message},
    model::{protocol::ws::response::WebSocketResponse, vo::chat::ChatSendResponseVO},
    ws::socket::MESSAGE_MAP,
};
use redis::AsyncCommands;
use rocket::serde::json::{Value, from_value};
use rocket_json_response::json_gettext::serde_json::json;

/**
聊天

数据传入格式:

```rust
use rbatis::rbdc::DateTime;
pub struct Message {
    /// 消息唯一id
    pub id: Option<i64>,
    ///  消息发送者id
    pub from_id: Option<i64>,
    /// 消息发送到的位置
    pub to_id: Option<i64>,
    /// 消息tag，决定消息的类型:1:群聊类型 2:私聊类型 3:树洞类型
    pub message_tag: Option<i32>,
    /// 消息的具体数据，通过解释引擎进行解释
    pub data: Option<String>,
    /// 消息类型 默认为0文本类型
    pub message_type: Option<i32>,
    ///  消息发送时间
    pub time: Option<DateTime>,
}
```
*/
pub async fn chat_send_controller(info: Value) -> WebSocketResponse {
    let value: Message = from_value(info.clone()).unwrap();
    match Message::insert(MYSQL_POOL.get().unwrap(), &value).await {
        Ok(_) => {
            // 添加数据至redis作为缓存
            unsafe {
                let temp = REDIS_POOL.get().unwrap().get().as_mut().unwrap();
                let mut temp_pool: Vec<String> = temp.get(value.to_id).await.unwrap();
                temp_pool.push(info.to_string());
                let _: () = temp.set(value.to_id, temp_pool).await.unwrap();
            }
            unsafe {
                MESSAGE_MAP
                    .get()
                    .unwrap()
                    .get()
                    .as_mut()
                    .unwrap()
                    .insert(value.to_id.unwrap(), true);
            }
            WebSocketResponse::success(Value::String(ChatSendResponseVO::success().msg))
        }
        Err(e) => WebSocketResponse::error(Value::String(e.to_string())),
    }
}

pub async fn chat_receive_request_controller(info: Value) -> WebSocketResponse {
    let value: Message = from_value(info.clone()).unwrap();
    let temp = unsafe {
        let temp = REDIS_POOL.get().unwrap().get().as_mut().unwrap();
        let mut temp_pool: Vec<String> = temp.get(value.to_id).await.unwrap();
        let result = temp_pool.first().unwrap().as_str().to_owned();
        temp_pool.remove(0);
        result
    };
    if temp.is_empty() {
        //TODO: 处理redis缓存信息空的情况
        return WebSocketResponse::error(json!("null"));
    }
    WebSocketResponse::success(Value::String(temp))
}
