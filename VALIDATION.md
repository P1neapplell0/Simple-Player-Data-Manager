# 验证记录

## 环境

- NeoForge：Minecraft 1.21.1 / NeoForge 21.1.234 / Java 21。
- Forge：使用本机已有 Minecraft 1.20.1 / Forge **47.4.4** / Java 17 / 官方 mappings。
- 两版均执行 `clean build`，生成运行、sources、javadoc jar；Forge 额外完成 `reobfJar`。
- 校验代码通过临时 Gradle init script 加入，正式 jar 不包含测试类、测试字段或测试结构。

## 基础校验（每版 29 项）

涵盖非零默认值、五种 NBT 类型、long 精度、空字符串与缺失值区分、错误 NBT 类型、非法 double、字符串长度/null、重复键、非法 namespace、注册表查询与不可修改快照。

存储验证涵盖同键 dirty 合并、最终值保留、增量应用、SERVER_ONLY 不产生 dirty、存档拷贝隔离、恢复后无 dirty、reset 删除镜像、同 tick reset→set、同步过滤私有/未知/非法字段以及完整快照清除旧值。

## 服务器运行校验（每版 16 项）

两版均实际启动 GameTestServer，并通过一个含 16 项断言的玩家数据测试：

- 使用服务端 FakePlayer 验证真实 Attachment / Capability 挂载及 API 读写。
- 验证默认值惰性读取、不存在字段写默认值不建记录、相等写入无操作。
- 验证 int 加法、bool 切换、double 加法、long 极值、空字符串。
- 验证 dirty 仅包含修改键、reset 删除显式存储并恢复默认值。
- 通过玩家 `saveWithoutId` / `load` 验证外层玩家 NBT 的真实持久化路径。
- NeoForge 使用 `ServerPlayer.restoreFrom` 验证死亡时 Attachment 复制。
- Forge 先使旧玩家 Capability 失效，再触发 Clone 复制，验证 revive、复制和再次失效完整路径。
- 验证新玩家持有独立状态，可继续修改。

结果：两版均输出 `All 1 required tests passed`，共 **90 项基础/玩家断言通过**。
Forge 运行测试发现并修复 LazyOptional 在旧玩家失效后无法通过 revive 恢复访问的问题；旧 Optional 仍然失效，提供者为后续合法 revive 创建新 Optional。

## 业务工程接入

另建两个不修改本库源码的临时业务工程，按 README 引用正式 jar 并编译五种字段声明及读写操作：
NeoForge 使用 `implementation files(...)`，Forge 使用 `flatDir` + `fg.deobf` 坐标。两个消费者均编译成功，Forge 使用离线模式。

## 尚未覆盖

未进行真实客户端与专用服务器的双端联机操作、网络抓包、UI 读取、多人重连、生产服务器异常断电或长时压力测试。
客户端镜像合并、过滤及删除逻辑已测试，但不应把 FakePlayer 服务端测试当作真实客户端收包端到端验证。
原版首次生成 server.properties、Forge 版本更新提示、终端特性和 assets URL 等环境警告不代表本库加载失败。

## 2026-11-08：Forge 死亡移除后的同步回归

CR - Build 的崩溃存档玩家处于 `Health=0`、`DeathTime=21` 状态。Forge 的玩家死亡 tick 会移除实体并使 Capability 失效，随后本库 END tick 自动 flush 仍访问旧玩家，触发 `Missing player data capability`。

修复仅针对 Forge 1.20.1：`PlayerDataManager.flush/sync` 在主线程检查后跳过 `isRemoved()` 玩家，不复活 Capability、不返回空存储、不清除 dirty 状态。正常玩家缺失 Capability 仍然报错。版本号保持 `1.0.0`。

通过临时 init script 注入 5 个 GameTest，在 Forge **47.4.4 / Java 17** 上验证：

1. 真实 `ServerPlayer.doTick` 从 `Health=0`、`DeathTime=19` 进入死亡移除并触发 END tick。
2. 玩家 NBT 带 `Health=0`、`DeathTime=21` 加载后运行真实 `doTick`，保留已有字段。
3. 已移除玩家显式 `flush/sync` 以及事件总线 END tick 不抛异常，不复活 Capability、不清除待同步字段。
4. 移除后 Clone 保留公开和服务端私有字段，新玩家可以修改、发送增量并执行重生快照。
5. 非移除玩家 Capability 异常失效仍然抛错，不掩盖挂载故障。

修复前 4 项失败，均为 `Missing player data capability`；修复后 5 项全部通过。
日志：`/tmp/spdm-death-before.log`、`/tmp/spdm-death-after.log`（临时文件，不保证长期保留）。

另在未修改的 NeoForge 1.21.1 分支实际运行 2 个 GameTest：死亡 tick 后读取 Attachment、同步及 Clone，以及死亡状态 NBT 加载后 tick，全部通过。
该异常由 Forge Capability 失效路径引起，NeoForge Attachment 不经过这一失效检查，因此本次不改动 NeoForge 代码或产物。
NeoForge 验证日志：`/tmp/spdm-neo-death-check.log`。上述测试不替代 CR 完整整合包的真人客户端进档验证。

修复后 Forge 执行 `clean build --offline` 成功，正式 jar 不包含临时回归测试类或结构，运行、sources、javadoc 三份产物均更新，版本号保持不变。
已将三份 Forge 产物替换到 CR - Build 的 `libs`，原产物保存在该工程 `build/spdm-backup-2026-11-08`。
CR 工程执行 `compileJava --offline --refresh-dependencies` 成功，并检查 ForgeGradle 的反混淆 jar 字节码，确认 `flush/sync` 都包含 `isRemoved()` 判断，避免同版本缓存仍使用旧实现。
未修改 CR 业务源码、玩家存档或其他依赖；仍需重新启动客户端验证原存档的实际进档体验。
