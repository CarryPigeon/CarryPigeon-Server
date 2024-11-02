use crate::dao::MYSQL_POOL;
use crate::dao::message::Message;
use rbatis::rbdc::Error;
use rbatis::rbdc::db::ExecResult;

pub async fn push_message_repository(message: &Message) -> Result<ExecResult, Error> {
    Message::insert(MYSQL_POOL.get().unwrap(), message).await
}
