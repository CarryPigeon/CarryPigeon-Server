#[macro_use]
extern crate rocket;

use carrypigeon_server::controller::account::user::user_register_controller;
use carrypigeon_server::controller::tree_hole::tree_hole_send_controller;
use carrypigeon_server::manager::ws::init_web_socket_manager;
use carrypigeon_server::utils::id::init_snow;
use carrypigeon_server::ws::init_ws_dispatcher;
use carrypigeon_server::ws::socket::websocket_service;
use tracing_appender::{non_blocking, rolling};
use tracing_subscriber::{
    Registry, filter::EnvFilter, fmt, layer::SubscriberExt, util::SubscriberInitExt,
};

#[rocket::main]
async fn main() -> Result<(), rocket::Error> {
    // 处理tracing输出和调用
    let env_filter =
    // 此处过滤了info以下的信息
    // 正式版时需要替换为warn
        Box::new(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")));
    // 输出到控制台中
    //let formatting_layer = Box::(fmt::layer().pretty().with_writer(std::io::stderr));
    let formatting_layer = Box::new(fmt::layer());

    // 输出到文件中
    let file_appender = Box::new(rolling::never("logs", "log.txt"));
    let (non_blocking_appender, _guard) = non_blocking(file_appender);
    let file_layer = Box::new(
        fmt::layer()
            .with_ansi(false)
            .with_writer(non_blocking_appender),
    );

    // 注册
    Registry::default()
        .with(env_filter)
        .with(formatting_layer)
        .with(file_layer)
        .init();

    // 初始化
    init_snow();
    init_web_socket_manager();
    init_ws_dispatcher();

    // connect database
    carrypigeon_server::dao::init_pool().await;

    let _rocket = rocket::build()
        //.mount("/authenticator", routes![post_authenticator])
        //.mount("/group", routes![new_group])
        .mount("/account/user", routes![user_register_controller])
        .mount("/tree_hole", routes![tree_hole_send_controller])
        .mount("/websocket", routes![websocket_service])
        //.mount("/service", routes![websocket_service])
        //.mount("/upload", routes![upload_file, retrieve_file, delete_file])
        .launch()
        .await?;
    Ok(())
}
