/*!
WebSocket连接相关客户端发送的响应协议

```json
{
    "code":,
    "id":,
    "data":,
}
```
code为标识码，用于标识请求是否处理成功，服务端主动发送的消息默认为SUCCESS_CODE

id为标识id，用于标识本地的请求，服务端主动发送的消息默认为-1<br/>
若为-1则需在data中标识客户端分路

data为可选的额外数据类型，用于传递相关数据，若仅用于声明请求成功或者失败可设为None
 */
use std::sync::OnceLock;

use crate::model::protocol::{ERROR_CODE, SUCCESS_CODE};
use rocket::serde::json::Value;
use rocket::serde::json::serde_json::json;
use rocket_json_response::ToJSON;
use serde::{Deserialize, Serialize};

/**
用于websocket用于数据返回
 */
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct WebSocketResponse {
    /// 状态码，用于表示请求成功或者失败
    pub code: u32,
    /// 请求id，用于本地客户端使用用于鉴别返回值，如果为-1则为服务端主动向客户端发送消息，例如其他用户发送了消息发到本地
    pub id: i64,
    /// 返回值携带的数据
    pub data: Option<Value>,
    /// 客户端分路,可选类型
    pub route: Option<String>,
}

impl WebSocketResponse {
    pub fn init() {
        let _ = WEBSOCKET_RESPONSE_ROUTE_ERROR.set(WebSocketResponse {
            code: ERROR_CODE,
            id: -1,
            data: Some(json!("no such route")),
            route: None,
        });
        let _ = WEBSOCKET_RESPONSE_CONTENT_STRUCTURE_ERROR.set(WebSocketResponse {
            code: ERROR_CODE,
            id: -1,
            data: Some(json!("the analysis of the json meet some wrong")),
            route: None,
        });
        let _ = WEBSOCKET_RESPONSE_ERROR.set(WebSocketResponse {
            code: SUCCESS_CODE,
            id: -1,
            data: None,
            route: None,
        });
    }
    pub fn success(data: Value) -> WebSocketResponse {
        WebSocketResponse {
            code: SUCCESS_CODE,
            id: -1,
            data: Some(data),
            route: None,
        }
    }

    pub fn error(data: Value) -> WebSocketResponse {
        WebSocketResponse {
            code: ERROR_CODE,
            id: -1,
            data: Some(data),
            route: None,
        }
    }

    pub fn send(data: Value, route: String) -> WebSocketResponse {
        WebSocketResponse {
            code: SUCCESS_CODE,
            id: -1,
            data: Some(data),
            route: Some(route),
        }
    }

    pub fn to_json(self) -> String {
        json!(self).to_json()
    }
}

/*标准的响应*/

/**
异常的route，用于分配路径失败使用
 */
pub static WEBSOCKET_RESPONSE_ROUTE_ERROR: OnceLock<WebSocketResponse> = OnceLock::new();
/**
异常的参数，用于参数分析失败时使用
 */
pub static WEBSOCKET_RESPONSE_CONTENT_STRUCTURE_ERROR: OnceLock<WebSocketResponse> =
    OnceLock::new();
/**
用于测试使用
 */
pub static WEBSOCKET_RESPONSE_ERROR: OnceLock<WebSocketResponse> = OnceLock::new();
