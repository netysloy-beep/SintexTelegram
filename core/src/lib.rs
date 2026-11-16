use grammers_client::{Client, Config, Update};
use grammers_session::Session;
use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use std::sync::OnceLock;

static mut API_ID: i32 = 0;
static mut API_HASH: String = String::new();
static CLIENT: OnceLock<Client> = OnceLock::new();

fn rt() -> tokio::runtime::Runtime {
    tokio::runtime::Builder::new_multi_thread().enable_all().build().unwrap()
}

#[no_mangle]
pub extern "system" fn Java_com_netysloy_rustgram_TgCore_init(
    env: JNIEnv, _c: JClass, api_id: jint, api_hash: JString,
) {
    unsafe {
        API_ID = api_id;
        API_HASH = env.get_string(&api_hash).unwrap().to_string_lossy().into_owned();
    }
}

#[no_mangle]
pub extern "system" fn Java_com_netysloy_rustgram_TgCore_login(
    env: JNIEnv, _c: JClass, phone: JString, code: JString,
) -> jstring {
    let phone = env.get_string(&phone).unwrap().to_string_lossy().into_owned();
    let code = env.get_string(&code).unwrap().to_string_lossy().into_owned();
    let (api_id, api_hash) = unsafe { (API_ID, API_HASH.clone()) };
    let res = rt().block_on(async move {
        let client = Client::connect(Config {
            session: Session::load_file_or_create("rustgram.session")?,
            api_id,
            api_hash,
            params: Default::default(),
        }).await?;
        client.request_login_code(&phone).await?;
        client.sign_in(&code).await?;
        let me = client.get_me().await?;
        let info = format!(
            "ID: {}\nЮзернейм: {}\nТелефон: {}\nПремиум: {}\n",
            me.id(), me.username().unwrap_or("-"),
            me.phone().unwrap_or("-"), me.premium().unwrap_or(false)
        );
        let _ = CLIENT.set(client);
        Ok::<String, Box<dyn std::error::Error>>(info)
    });
    env.new_string(res.unwrap_or_else(|e| format!("Ошибка: {e}")))
        .unwrap().into_raw()
}

/// Узнать ID и публичную инфу другого пользователя: @username.
#[no_mangle]
pub extern "system" fn Java_com_netysloy_rustgram_TgCore_lookup(
    env: JNIEnv, _c: JClass, target: JString,
) -> jstring {
    let target = env.get_string(&target).unwrap().to_string_lossy().into_owned();
    let Some(client) = CLIENT.get() else {
        return env.new_string("Сначала войди в аккаунт").unwrap().into_raw();
    };
    let client = client.clone();
    let res = rt().block_on(async move {
        let user = client.resolve_username(target.trim_start_matches('@')).await?;
        Ok::<String, Box<dyn std::error::Error>>(format!(
            "ID: {}\nИмя: {}\nЮзернейм: {:?}\nПремиум: {:?}\nБыл(а): {:?}\n",
            user.id(), user.first_name().unwrap_or(""),
            user.username(), user.premium(), user.last_seen()
        ))
    });
    env.new_string(res.unwrap_or_else(|e| format!("Ошибка: {e}")))
        .unwrap().into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_netysloy_rustgram_TgCore_startArchive(_env: JNIEnv, _c: JClass) {
    std::thread::spawn(|| {
        let Some(client) = CLIENT.get() else { return };
        let client = client.clone();
        rt().block_on(async move {
            loop {
                let (updates, _) = client.next_update_batch().await.unwrap();
                for u in updates {
                    let line = match u {
                        Update::NewMessage(m) => format!(
                            "[СОХРАНЕНО] {} | {} | {}\n",
                            m.date(), m.chat().name().unwrap_or_default(), m.text().unwrap_or_default()
                        ),
                        Update::DeletedMessages(d) => format!("[УДАЛЕНО] {:?}\n", d.messages),
                        _ => continue,
                    };
                    tokio::fs::OpenOptions::new()
                        .create(true).append(true)
                        .open("archive.log").await
                        .and_then(|mut f| {
                            use tokio::io::AsyncWriteExt;
                            Ok(f.write_all(line.as_bytes()).await?)
                        }).ok();
                }
            }
        });
    });
}
