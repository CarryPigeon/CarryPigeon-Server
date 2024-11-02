use crate::manager::ws::WebSocketManager;
use crate::model::chat::CPMessageTrait;
use crate::model::chat::tag::tree_hole::TreeHoleMessage;
use crate::model::chat::r#type::text::CPTextMessageData;
use crate::model::dto::tree_hole::TreeHoleSendDTO;
use crate::repository::account::friend::get_friends_repository;
use crate::service::account::user::user_authority_check_service;
use crate::service::chat::public::push_message_and_notice_all_service;

pub async fn tree_hole_send_service(token: String, data: TreeHoleSendDTO) -> Result<(), String> {
    // 校验用户权限
    if !user_authority_check_service(&data.user_id, token).await {
        return Err("authority check error".to_string());
    }
    // 数据持久化并通知全局
    // 获取相关用户(所有好友)
    let friends = get_friends_repository(&data.user_id).await;
    // 将friends集合转化为id数组
    let mut friend_ids = Vec::with_capacity(friends.len());
    for friend in friends {
        // 获取用户id
        let id = if friend.person_1.unwrap() == data.user_id {
            friend.person_2.unwrap()
        } else {
            friend.person_1.unwrap()
        };
        // 判断用户是否在线
        if WebSocketManager::is_online(&id).await {
            friend_ids.push(id);
        }
    }
    let tree_hole_message = TreeHoleMessage {
        from_id: data.user_id,
        data: Box::new(CPTextMessageData::new(&data.data)),
    };
    let message = tree_hole_message.to_message();
    push_message_and_notice_all_service(message, friend_ids).await
}
