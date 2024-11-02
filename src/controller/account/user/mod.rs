use crate::dao::{MYSQL_POOL, account::user::User};
use crate::model::protocol::http::response::HttpResponse;
use crate::model::vo::account::user::{UserRegisterResponseVo, UserRegisterVo};
use crate::service::account::user::user_register_service;
use crate::ws::socket::MESSAGE_MAP;
use rocket::post;
use rocket::serde::json::Json;
use rocket_json_response::JSONResponse;

/**
新建一个账户

数据传入格式：

```json
{
    "username":"",
    "password":""
}
```
 */
#[post("/register", data = "<info>")]
pub async fn user_register_controller(
    info: Json<UserRegisterVo>,
) -> JSONResponse<'static, UserRegisterResponseVo> {
    let result = user_register_service(info.clone().into_inner().to_dto()).await;
    match result {
        Ok(_) => {
            let init_user_data = User::select_by_column(
                MYSQL_POOL.get().unwrap(),
                "username",
                info.into_inner().username,
            )
            .await
            .unwrap();
            unsafe {
                MESSAGE_MAP
                    .get()
                    .unwrap()
                    .get()
                    .as_mut()
                    .unwrap()
                    .insert(init_user_data.first().unwrap().id.unwrap(), false);
            }
            HttpResponse::success(UserRegisterResponseVo::success())
        }
        Err(e) => HttpResponse::error(UserRegisterResponseVo::error(&e)),
    }
}
