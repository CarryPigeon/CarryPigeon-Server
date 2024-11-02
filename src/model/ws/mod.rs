use crate::model::protocol::ws::response::WebSocketResponse;
use rocket::futures::SinkExt;
use rocket::futures::stream::SplitSink;
use rocket_ws::Message;
use rocket_ws::stream::DuplexStream;
use std::sync::Arc;
use tokio::sync::Mutex;

/**
ws通道管理的数据结构
包含用于权限校验的token和通信的sender
 */
pub struct WSUser {
    pub token: String,
    pub sender: Arc<Mutex<CPSender>>,
}

impl WSUser {
    pub fn new(token: String, sender: Arc<Mutex<CPSender>>) -> WSUser {
        WSUser { token, sender }
    }
}

/**
对ws的sender进行的封装
 */
pub struct CPSender {
    sender: SplitSink<DuplexStream, Message>,
}

impl CPSender {
    pub fn new(sender: SplitSink<DuplexStream, Message>) -> CPSender {
        CPSender { sender }
    }

    pub async fn send_json(&mut self, json: String) {
        let _ = self.sender.send(Message::Text(json)).await;
    }

    pub async fn send_pong(&mut self) {
        let _ = self
            .sender
            .send(Message::Pong("pong".as_bytes().to_vec()))
            .await;
    }

    pub async fn send_ws_data(&mut self, response: WebSocketResponse) {
        self.send_json(response.to_json()).await
    }
}
