use crate::dao::MYSQL_POOL;
use crate::dao::account::group::GroupMember;

/**
获取一个群聊所有用户的id合集
 */
pub async fn get_group_members_repository(group_id: &i64) -> Vec<i64> {
    let result = GroupMember::select_all_member(MYSQL_POOL.get().unwrap(), group_id).await;
    match result {
        Ok(members) => {
            let mut member_ids = Vec::with_capacity(members.len());
            for member in members {
                member_ids.push(member.user_id.unwrap());
            }
            member_ids
        }
        Err(e) => {
            tracing::error!("{}", e.to_string());
            Vec::new()
        }
    }
}
