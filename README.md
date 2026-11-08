# Simple Player Data Manager

独立的玩家数据基础库，提供强类型、自动持久化、服务端 → 所属客户端的自动增量同步。

| 分支 | Minecraft | 加载器 | Java |
| --- | --- | --- | --- |
| `1.21.1` | 1.21.1 | NeoForge 21.1.234+（21.1 系列） | 21 |
| `1.20.1` | 1.20.1 | Forge **47.4.4** | 17 |

公共 API 位于 `com.p1nero.misc.simple_player_data_manager.api`，入口为 `PlayerDataManager`。
双端均须安装本库和声明数据键的业务 mod，使用相同键、类型、默认值和同步模式。

## 快速接入

### 1. 添加依赖

把对应版本的运行 jar 放入业务工程的 `libs`；不要使用 sources/javadoc jar 运行游戏。

NeoForge 1.21.1：

```groovy
dependencies {
    implementation files('libs/simple_player_data_manager-1.0.0+mc1.21.1-neoforge.jar')
}
```

Forge 1.20.1（ForgeGradle）：

```groovy
repositories {
    flatDir { dirs 'libs' }
}
dependencies {
    implementation fg.deobf('local:simple_player_data_manager:1.0.0+mc1.20.1-forge')
}
```

在业务 mod 的 `neoforge.mods.toml` 中声明必需依赖：

```toml
[[dependencies.your_mod_id]]
modId="simple_player_data_manager"
type="required"
versionRange="[1.0.0,2.0.0)"
ordering="AFTER"
side="BOTH"
```

Forge 的 `mods.toml` 将 `type="required"` 换成 `mandatory=true`。
依赖 jar 只是开发/运行依赖，不需要也不应该把库的类打进业务 mod。
ForgeGradle 不能反混淆 `files(...)` 依赖，所以 Forge 接入必须使用上述 flatDir 坐标或真正的 Maven 坐标。

### 2. 在双端的 mod 构造期间注册

```java
import com.p1nero.misc.simple_player_data_manager.api.*;

public final class ExamplePlayerData {
    private static final PlayerDataRegister REGISTER = PlayerDataRegister.create("examplemod");

    public static final BoolData INTRO_DONE = REGISTER.boolData("intro_done");
    public static final IntData LEVEL = REGISTER.intData("level", 1);
    public static final LongData COINS = REGISTER.longData("coins");
    public static final DoubleData ENERGY = REGISTER.doubleData("energy", 100.0);
    public static final StringData TITLE = REGISTER.stringData("title", "新手");
    public static final LongData SECRET_SEED = REGISTER.longData("secret_seed", 0L, SyncMode.SERVER_ONLY);

    public static void initialize() {}
}
```

在你的 `@Mod` 构造器调用 `ExamplePlayerData.initialize()`，确保类初始化；仅声明一个从未引用的类不会注册数据。
注册立即生效，不需要绑定事件总线；不要每次访问玩家、登录或在客户端专属事件中重新注册。
同一个 `namespace:path` 只允许注册一次，即使类型和默认值相同，重复注册也会立即报错。
`PlayerDataRegister` 是命名空间工厂，不是 Minecraft 的 `DeferredRegister`。

也可直接声明单个键：

```java
BoolData.create(ResourceLocation.fromNamespaceAndPath("examplemod", "intro_done"), false);
```

本库锁定的 Forge 47.4.4 已回移 `ResourceLocation.fromNamespaceAndPath`，因此两版可使用相同写法。
每种 `create` 都有 `(id)`、`(id, defaultValue)`、`(id, defaultValue, syncMode)` 重载。
不提供无键的 `create()`：持久化数据必须有稳定、唯一的 ResourceLocation 身份。

### 3. 读取与修改

```java
int level = ExamplePlayerData.LEVEL.get(player);
String title = ExamplePlayerData.TITLE.get(player);

ExamplePlayerData.INTRO_DONE.markTrue(serverPlayer);
ExamplePlayerData.LEVEL.increment(serverPlayer);
ExamplePlayerData.COINS.add(serverPlayer, 500L);
ExamplePlayerData.ENERGY.update(serverPlayer, current -> Math.max(0.0, current - 5.0));
ExamplePlayerData.TITLE.set(serverPlayer, "探索者");
ExamplePlayerData.TITLE.reset(serverPlayer);
```

`get` 可在两端调用，客户端读取最近收到的服务端镜像；尚未同步或键未存储时返回声明默认值。
`set/update/add/increment/reset/toggle/markTrue/markFalse` 必须在**逻辑服务端主线程**调用。
错误侧或异步线程调用会抛出 `IllegalStateException`，而不是偷偷写入客户端临时数据。
异步任务应使用 `server.execute(() -> ...)` 回到服务端线程再访问玩家。
客户端按钮需要业务 mod 自己发送 C2S 请求，由服务端校验权限、范围、冷却后调用本 API。
本库故意不接受“客户端任意写玩家字段”的网络包。

## API 速查

| API | 含义 |
| --- | --- |
| `key.id()/defaultValue()/syncMode()` | 不可变字段定义 |
| `key.get(player)` | 获取有效值；缺失/类型不符/非法值返回默认值 |
| `key.set(player, value)` | 写入；有实际状态变化返回 true |
| `key.update(player, operator)` | 主线程读改写，返回新值 |
| `key.reset(player)` | 删除显式存储，恢复默认值；存在时返回 true |
| `key.isPresent(player)` | 是否存在符合声明类型及约束的显式存储 |
| `BoolData.toggle/markTrue/markFalse` | 切换/设置布尔值 |
| `IntData/LongData.add/increment` | 加法；溢出抛出 `ArithmeticException` |
| `DoubleData.add` | 浮点加法；非有限结果抛出 `IllegalArgumentException` |
| `PlayerDataManager.get/set/reset/isPresent` | 与键实例方法等价的统一入口 |
| `PlayerDataManager.find(id)` | 按 ResourceLocation 查找已注册键，未注册返回 null |
| `PlayerDataManager.registeredKeys()` | 不可修改的注册表快照，顺序未定义 |
| `PlayerDataManager.flush(serverPlayer)` | 立即发送待同步的键，通常无需手动调用 |
| `PlayerDataManager.sync(serverPlayer)` | 强制完整快照，通常无需手动调用 |

默认值：bool=false、int=0、long=0L、double=0.0、String=""。
字符串不允许 null，最多 2048 个 Java UTF-16 code units；空字符串是合法值，不会误当成“缺失”。
double 拒绝 NaN 和正负无穷。键字符串最多 256 字符，整个进程最多 4096 个注册键。
整数/长整数不互相隐式转换，避免更改类型时悄悄截断旧数据。

## 持久化、同步与性能

1. **存储**：玩家实例持有独立的 NBT 键值表。1.21.1 使用可序列化 NeoForge Attachment（`copyOnDeath`）；1.20.1 使用 Forge Capability 和 `ICapabilitySerializable<CompoundTag>`。
2. **保存**：随原版玩家存档保存，无每 tick 磁盘 IO。登出、正常关服、重启后恢复；进程异常终止仍受原版最近一次保存时机限制。
3. **生命周期**：登录、重生、返回末地、切换维度后自动发送所属玩家快照。死亡时复制数据，不因 `keepInventory` 设置而丢失。
4. **普通修改**：只将发生变化的键加入 dirty NBT；同 tick 对同键多次写入只保留最终值，玩家 tick 末发送。不会遍历完整玩家数据；相同值写入不产生网络流量。
5. **重置**：发送单键删除标记，客户端删除对应镜像；`isPresent` 和默认值语义在两端一致。
6. **分批**：每包最多 32 个键；在当前类型和长度限制下控制包大小，避免完整快照无限增长成一个大包。首包清空旧镜像，其余按序合并。
7. **可见性**：`OWNER` 只发给该玩家，不广播给旁观者或其他玩家；`SERVER_ONLY` 仅持久化，客户端始终读默认值。
8. **数据隔离**：不使用全局 UUID → 玩家数据缓存；只保留进程级不可变字段注册表。不会跨服务器复用客户端镜像。
9. **线程**：写入限定服务器主线程；读取也应在对应逻辑侧的主线程执行。`update` 是主线程顺序操作，不是跨线程事务。

默认值惰性提供，不会在玩家加入时把所有默认字段写进存档。不存在的键写入默认值不会新建记录。
已经存储的键显式写回默认值仍保留记录；需要恢复“未设置”状态时用 `reset`。
同步延迟通常不超过一个 tick；若确实需要在随后发出的业务 S2C 包前刷新镜像，可以先 `flush`。
Forge 版 `flush/sync` 会跳过已经移除的玩家，避免死亡流程使 Capability 失效后仍在 END tick 访问。
这不会复活旧 Capability、清空待同步状态或改变死亡复制；新玩家仍在重生时接收完整快照。
正常玩家缺失 Capability 仍会报错，业务读写也不应继续使用已经移除的旧玩家实例。
多个字段不是网络事务，大量字段可能跨包；不要依赖中间包到达时的多字段原子性。
客户端不能据此读取其他玩家的数据；这不是共享记分板或全服数据同步库。

NBT 保存所有已有键，包括当前未注册的旧业务键，避免临时移除业务 mod 时删档；同步只包含已注册且类型合法的 OWNER 字段。
双端注册表不进行 schema 协商；注册必须在初始同步前完成且保持一致。客户端缺失/类型不匹配的键会忽略，而不会自动动态注册。
`internal` 和 `network` 包、`DataKey.encode/decode/accepts` 是实现接口，不是业务存储 API，不要直接修改底层状态绕过 dirty 跟踪。

## 与原 CIV 系统的差异

原系统将业务布尔标志集中在管理器中，通过 Attachment 保存 NBT；布尔值单键同步，但 long/string/删除等操作发送完整玩家 NBT。
生命周期同步散落于业务事件，字符串空值和默认值容易混淆，布尔字段的声明默认值也没有完整进入读取路径。

本库移除所有业务标志和调试命令，拆分为 `api`、`internal`、`network`，用五种强类型字段统一同步流程。
ResourceLocation 使不同业务 mod 可共享同一存储系统而不冲突。业务如需 debug 命令，可遍历 `registeredKeys()` 自行接入。
这是独立库，**不会自动导入旧 CIV attachment 或重命名旧 string key**；迁移原存档需业务 mod 读取旧数据，映射后在服务端逐键 set。
两个分支的 API 和 NBT 键值格式一致，但 Minecraft 世界降级以及不同加载器外层玩家存储容器不属于兼容保证。

## 构建与发布

```bash
git switch 1.21.1
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./gradlew build

git switch 1.20.1
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew build
```

Forge 固定使用本机已有的 **1.20.1-47.4.4**，官方 mappings、ForgeGradle 6.0.54、Gradle 8.8；不需要迁移到新 Forge 版本。
每个分支生成运行 jar、sources jar 和 javadoc jar。产物名包含 Minecraft 和加载器版本，普通 `build` 不会删除另一版本的产物；`clean` 会。
两条分支提交后，可在任一分支执行 `bash scripts/build-all.sh`，以临时 detached worktree 构建两版并汇总至当前根目录的 `build/libs`。
该脚本构建的是**分支已提交代码**，不会包含未提交修改。

运行验收建议：专用服务器登录 → 两端读取默认值 → 同 tick 多次写入 → 检查单键合并 → reset → 死亡/末地返回/换维度 → 重连/重启 → 检查持久化与私有字段；单机也应重复上述流程。
编译通过不等价于已经完成真人客户端与服务器的联机验证。
