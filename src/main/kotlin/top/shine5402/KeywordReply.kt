package top.shine5402

import net.mamoe.mirai.console.command.registerCommand
import net.mamoe.mirai.console.plugins.Config
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.console.plugins.ToBeRemoved
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.data.source

object KeywordReply : PluginBase() {
    val config = loadConfig("settings.yml")

    private fun loadConfig(config: Config) {
        val keywordRulesConfigSection = if (config.exist("rules")) config.getConfigSectionList("rules") else null
        keywordRules.clear()
        keywordRules.addAll(keywordRulesConfigSection?.map {
            KeywordRuleFactory.fromConfigSection(it)
        }?.toMutableList() ?: mutableListOf())
    }

    private fun saveConfig() {
        config.set("rules", keywordRules.map { it.toConfigSection() })
        config.save()
    }

    private val keywordRules: MutableList<KeywordRule> = mutableListOf()

    override fun onLoad() {
        super.onLoad()
        loadConfig(config)
        logger.info("关键字配置已经被读取。共读取了${keywordRules.count()}条规则。")
    }

    override fun onEnable() {
        super.onEnable()

        logger.info("关键字回复已被启用。")

        subscribeAlways<MessageEvent> { event ->
            for (rule in keywordRules) {
                for (message in event.message) {
                    if (rule.match(message.content, event.source)) {
                        when (event) {
                            is GroupMessageEvent -> {
                                event.group.sendMessage(rule.reply)
                            }
                            is FriendMessageEvent -> {
                                event.sender.sendMessage(rule.reply)
                            }
                            is TempMessageEvent -> {
                                event.sender.sendMessage(rule.reply)
                            }
                        }
                    }
                }
            }
        }

        registerCommands()
    }

    private fun registerCommands() {
        registerCommand {
            name = "keywordAdd"
            alias = listOf("guanjianzitianjia", "关键字添加", "gjztj")
            description = "添加一条关键字回复规则"
            usage = "格式：/keywordAdd <类型> <关键字> <回复> <(可选的)以分号分隔的群号列表>\n" +
                    "keywordAdd 命令有“guanjianzitianjia”、“关键字”、“gjztj”几个别名。\n" +
                    "目前类型支持：exact（完全匹配）、vague（模糊匹配）、regex（正则匹配）\n" +
                    "类型的中文和中文去掉“匹配”也是其别名。\n" +
                    "请注意关键字和回复中不能出现空格（因为要被命令解析），如果有需要，可以自己修改settings.yml文件中存储的规则。\n" +
                    "如果不提供群列表，那么将会响应所有群的消息。\n" +
                    "所有关键字回复都会响应私聊·临时消息。"
            onCommand {
                if (it.count() !in 3..4)
                    return@onCommand false
                val type = it[0]
                lateinit var _type: String
                val keyword = it[1]
                val reply = it[2]
                val groupsString = it.getOrElse(3) { "" }

                //处理类型别名
                when (type) {
                    "exact", "完全匹配", "完全" -> _type = "exact"
                    "vague", "模糊匹配", "模糊" -> _type = "vague"
                    "regex", "正则匹配", "正则" -> _type = "regex"
                    else -> return@onCommand false
                }
                //处理群号
                val groups = groupsString.split(";").map { it.toLongOrNull() ?: 0 }.filter { it != 0L }

                keywordRules.add(KeywordRuleFactory.create(_type, keyword, reply, groups))
                val logMessage = "关键字添加成功。\n" + keywordRules.last().toStringHumanFriendly()
                sendMessage(logMessage)
                logger.info(logMessage)
                return@onCommand true
            }
        }

        registerCommand {
            name = "keywordList"
            alias = listOf("列出关键字", "liechuguanjianzi", "lcgjz")
            description = "列出当前注册的所有关键字回复规则"
            usage = "格式：/keywordList\n" +
                    "keywordList 有“列出关键字”、“liechuguanjianzi”、“lcgjz”几个别名。\n" +
                    "目前版本暂不支持筛选。\n" +
                    "输出格式为 规则序号| 规则说明"

            onCommand {
                if (it.count() != 0)
                    return@onCommand false
                if (keywordRules.isEmpty()) {
                    sendMessage("目前没有关键字回复规则。")
                    return@onCommand true
                }
                sendMessage(keywordRules.mapIndexed { i, rule ->
                    "${i + 1}| ${rule.toStringHumanFriendly()}"
                }.joinToString("\n"))
                return@onCommand true
            }
        }

        registerCommand {
            name = "keywordRemove"
            alias = listOf("关键字删除", "guanjianzishanchu", "gjzsc")
            description = "删除一条关键字回复规则"
            usage = "格式：/keywordRemove <规则序号>\n" +
                    "keywordRemove 有“关键字删除”、“guanjianzishanchu”、“gjzsc”几个别名。\n" +
                    "序号请参照keywordList输出的序号。"
            onCommand {
                if (it.count() != 1)
                    return@onCommand false
                val id = (it[0].toIntOrNull() ?: 0) - 1
                if (id == -1 && id !in 0..keywordRules.count())
                    return@onCommand false
                sendMessage("该规则已经被删除：${keywordRules.removeAt(id).toStringHumanFriendly()}")
                return@onCommand true
            }
        }
        registerCommand {
            name = "keywordSave"
            alias = listOf("保存关键字", "baocunguanjianzi", "bcgjz")
            description = "触发一次关键字配置保存"
            usage = "为了方便开发·维护而设的命令。可以使用该命令要求插件保存关键字配置到配置文件。\n" +
                    "keywordSave 有“保存关键字”、“baocunguanjianzi”、“bcgjz”几个别名。"
            onCommand {
                if (it.count() != 0)
                    return@onCommand false
                saveConfig()
                sendMessage("关键词配置保存完成。")
                return@onCommand true
            }
        }

        registerCommand{
            name = "keywordLoad"
            alias = listOf("读取关键字", "duquguanjianzi", "dqgjz")
            description = "触发一次关键字配置读取"
            usage = "为了方便开发·维护而设的命令。可以使用该命令要求插件读取配置文件中的关键字配置。\n" +
                    "keywordLoad 有“读取关键字”、“duquguanjianzi”、“dqgjz”几个别名。" +
                    "请注意，使用此命令会导致没有保存的关键字配置丢失。"
            onCommand {
                if (it.count() != 0)
                    return@onCommand false
                    loadConfig(config)
                return@onCommand true
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        saveConfig()
    }
}