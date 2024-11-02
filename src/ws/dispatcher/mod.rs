use crate::model::protocol::ws::request::WebSocketRequest;
use crate::model::protocol::ws::response::{WEBSOCKET_RESPONSE_ROUTE_ERROR, WebSocketResponse};
use crate::ws::WS_DISPATCHER;
use rocket::serde::json::Value;
use std::collections::HashMap;
use std::future::Future;
use std::pin::Pin;
use std::sync::Arc;

// 需要引入的方法
use crate::controller::chat::message::chat_send_controller;
/**
请求分发
*/
pub async fn ws_dispatcher(request_data: WebSocketRequest) -> WebSocketResponse {
    let handler = WS_DISPATCHER
        .get()
        .unwrap()
        .dispatch(&request_data.route)
        .await;
    match handler {
        None => WEBSOCKET_RESPONSE_ROUTE_ERROR.get().unwrap().clone(),
        Some(handler) => handler.call((request_data.data,)).await,
        //handler.call((request_data.data,)).await,
    }
}

/**
websocket请求分发器，内部维护了一张路径的hash表，且初始化后不应该对路径进行更新，只读操作
*/
#[allow(clippy::type_complexity)]
pub struct WebSocketDispatcher {
    pub route_map: HashMap<
        String,
        Arc<dyn Fn(Value) -> Pin<Box<dyn Future<Output = WebSocketResponse> + Send>> + Send + Sync>,
    >,
}

impl WebSocketDispatcher {
    pub fn new() -> WebSocketDispatcher {
        let mut result = WebSocketDispatcher {
            route_map: HashMap::new(),
        };
        result.attach_route("chat", |x| Box::pin(chat_send_controller(x)));
        result
    }
    /**
    获取路径的引用
    */
    pub async fn dispatch(
        &self,
        path: &str,
    ) -> Option<
        &Arc<
            dyn Fn(Value) -> Pin<Box<dyn Future<Output = WebSocketResponse> + Send>> + Send + Sync,
        >,
    > {
        self.route_map.get(path)
    }

    /**
    注册route，应只在初始化时被调用
    */
    pub fn attach_route(
        &mut self,
        route: &str,
        handler: impl Fn(Value) -> Pin<Box<dyn Future<Output = WebSocketResponse> + Send>>
        + Send
        + Sync
        + 'static,
    ) {
        self.route_map.insert(route.to_string(), Arc::new(handler));
    }
}

impl Default for WebSocketDispatcher {
    fn default() -> Self {
        Self::new()
    }
}
