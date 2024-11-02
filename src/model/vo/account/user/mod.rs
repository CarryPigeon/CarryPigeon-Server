use crate::model::dto::account::user::{UserLoginDTO, UserRegisterDTO};
use rocket::FromForm;
use rocket::serde::{Deserialize, Serialize};
use rocket_json_response::serialize_to_json;

/**
注册账户的数据结构，用与controller接收数据
 */
#[derive(FromForm, Clone, Debug, Deserialize, Serialize)]
pub struct UserRegisterVo {
    pub username: String,
    pub password: String,
}

impl UserRegisterVo {
    /**
    用于当前vo转化为controller与service间的数据结构
    */
    pub fn to_dto(self) -> UserRegisterDTO {
        UserRegisterDTO {
            username: self.username,
            password: self.password,
        }
    }
}

/**
注册账户返回json结构
*/
#[derive(Clone, Debug, Serialize)]
pub struct UserRegisterResponseVo {
    pub mes: String,
}
serialize_to_json!(UserRegisterResponseVo);

impl UserRegisterResponseVo {
    pub fn success() -> UserRegisterResponseVo {
        UserRegisterResponseVo {
            mes: "register success".to_string(),
        }
    }
    pub fn error(msg: &str) -> UserRegisterResponseVo {
        UserRegisterResponseVo {
            mes: "register error,msg: ".to_string() + msg,
        }
    }
}

/**
用户登录，用于建立websocket连接
 */
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct UserLoginVo {
    /// 用户名
    pub username: String,
    /// 密码
    pub password: String,
}

impl UserLoginVo {
    /**
    用于当前vo转化为controller与service间的数据结构
     */
    pub fn to_dto(self) -> UserLoginDTO {
        UserLoginDTO {
            username: self.username,
            password: self.password,
        }
    }
}

/**
用户登录的返回类型
 */
#[derive(Clone, Debug, Deserialize, Serialize)]
pub struct UserLoginResponseVo {
    pub token: String,
}
