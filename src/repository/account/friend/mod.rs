use crate::dao::MYSQL_POOL;
use crate::dao::account::friend::Friend;

/**
或者指定用户的所有以通过好友申请的好友
 */
pub async fn get_friends_repository(user_id: &i64) -> Vec<Friend> {
    let result = Friend::select_all_friends_by_id(MYSQL_POOL.get().unwrap(), user_id).await;
    match result {
        Ok(friends) => friends,
        Err(e) => {
            // 输出错误日志
            tracing::error!("{}", e.to_string());
            Vec::new()
        }
    }
}

/**
通过好友关系id获取其好友关系的实体结构体
 */
pub async fn get_friend_repository(friend_id: &i64) -> Result<Friend, String> {
    let result = Friend::select_by_column(MYSQL_POOL.get().unwrap(), "id", friend_id).await;
    match result {
        Ok(friend) => {
            if friend.is_empty() {
                let error_msg = format!("no such friend id:{}", friend_id);
                tracing::error!("{}", &error_msg);
                return Err(error_msg);
            }
            Ok(friend[0].clone())
        }
        Err(e) => Err(e.to_string()),
    }
}
