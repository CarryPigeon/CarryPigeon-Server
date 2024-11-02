use crate::dao::MYSQL_POOL;
use crate::dao::account::user::User;

/**
通过用户名获取用户
*/
pub async fn select_user_by_name_repository(user_name: &str) -> Vec<User> {
    let result = User::select_by_column(MYSQL_POOL.get().unwrap(), "username", user_name).await;
    match result {
        Ok(users) => users,
        Err(e) => {
            tracing::error!(
                "there is some wrong in select users by name,error msg:{:?}",
                e
            );
            vec![]
        }
    }
}

/**
新增用户
*/
pub async fn insert_user_repository(user: User) -> bool {
    let result = User::insert(MYSQL_POOL.get().unwrap(), &user).await;
    match result {
        Ok(_) => true,
        Err(e) => {
            tracing::error!(
                "there is some wrong in insert user by name,error msg:{:?}",
                e
            );
            tracing::error!("error user data:{:?}", user);
            false
        }
    }
}
