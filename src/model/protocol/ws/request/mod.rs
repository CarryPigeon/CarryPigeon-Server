/*!
WebSocket连接相关客户端发送的请求协议

```json
{
    "request_id":,
    "route":,
    "data":,
}
```

request_id为消息id，用于客户端本地进行异步处理，由本地通过雪花算法生成

route 为请求路径，用于标识该请求应该使用那个处理函数进行处理

data为请求中的具体数据，为rocket::serde::json::Value，具体的处理函数自己决定如何对其进行处理
 */

use rocket::serde::json::{Value, serde_json};
use rocket::serde::{Deserialize, Serialize};

/**
websocket数据模型，所有通过websocket的消息都必须满足此模型
 */
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct WebSocketRequest {
    /// 消息id，用于客户端进行本地异步处理
    pub request_id: i64,
    /// route 用于进行路径分配
    pub route: String,
    /// 具体的数据
    pub data: Value,
}

impl WebSocketRequest {
    pub fn new(text: &str) -> serde_json::Result<WebSocketRequest> {
        serde_json::from_str(text)
    }
}
