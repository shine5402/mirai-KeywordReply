package top.shine5402

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.registerCommand
import net.mamoe.mirai.console.plugins.PluginBase
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.TempMessageEvent
import net.mamoe.mirai.message.data.content
import kotlin.properties.Delegates

object KeywordReply : PluginBase() {

    val keywordRules: MutableList<KeywordRule> = mutableListOf()
    private val keywordRulesKeywordList
        get() = keywordRules.map { it.keyword }

    fun findRuleByKeyword(keyword: String): KeywordRule? {
        return findRulesByKeyword(keyword).getOrNull(0)
    }

    fun findRulesByKeyword(keyword: String): List<KeywordRule> {
        return keywordRulesKeywordList.filter { it == keyword }
            .map { keywordRulesKeywordList.indexOf(it) }
            .map { keywordRules[it] }
    }

    override fun onLoad() {
        super.onLoad()
        KeywordReplyConfig.loadConfig()
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

    private val possibleConflictModes = listOf(
        "merge", "cover", "keep", "add",
        "合并", "覆盖", "保持", "追加"
    )

    private fun registerCommands() {
        registerCommand {
            name = "keywordAdd"
            alias = listOf("tianjiaguanjianzi", "添加关键字", "tjgjz")
            description = "添加一条关键字回复规则"
            usage = "格式：/keywordAdd <类型> <关键字> <回复> <(可选的)以分号分隔的群号列表，此条规则的开启群> <(可选的)冲突模式>\n" +
                    "请不要输入尖括号，尖括号只是为了让帮助中的参数更明显。\n" +
                    "keywordAdd 命令的别名：“tianjiaguanjianzi”、“添加关键字”、“tjgjz”\n" +
                    "目前类型支持：exact（完全匹配）、vague（模糊匹配）、regex（正则匹配）\n" +
                    "类型的对应中文和中文去掉“匹配”也是其别名。\n" +
                    "请注意关键字和回复中不能出现空格（因为要被命令解析），如果有需要，可以自己修改settings.yml文件中存储的规则。\n" +
                    "如果不提供群列表，那么将会响应所有群的消息。\n" +
                    "所有关键字回复都会响应私聊·临时消息。\n" +
                    "冲突模式是指在关键字和类型相同时，插件应如何处理这种情况。" +
                    "可选值为：merge（合并）、cover（覆盖）、keep（保持）、add（追加）。默认值是 merge。\n" +
                    "merge 会将对应的新回复合并到关键字的回复列表中，cover 会使用新回复覆盖原先设置的回复，keep 则保持原有的回复，" +
                    "add 则是将其作为独立的一条新回复规则（将与旧的同时触发）。\n" +
                    "如果现存规则中只有开启群不同、关键词相同的规则，无论指派什么冲突模式，插件都会将其视为add。" +
                    "只要现存规则中有开启群相同、关键词也相同的规则，那么就会遵循指定设置。" +
                    "且多条情况下，会对每一条都进行相应处理。\n"

            onCommand {
                if (it.count() !in 3..5)
                    return@onCommand false
                //读取用户所提供的参数
                val type = it[0]
                val keyword = it[1]
                val replies = mutableListOf(it[2])
                var groupsString: String = ""
                var conflictModeString: String = "merge"
                fun judgeParameter4() {
                    when (it[3]) {
                        in possibleConflictModes -> {
                            conflictModeString = it[3]
                        }
                        else -> groupsString = it[3]
                    }
                }

                when (it.count()) {
                    4 -> judgeParameter4()
                    5 -> {
                        groupsString = it[3]
                        conflictModeString = it[4]
                    }
                }

                return@onCommand doAdd(type, groupsString, conflictModeString, keyword, replies)
            }
        }

        registerCommand {
            name = "keywordAddMultipleReply"
            alias = listOf("添加多回复关键字", "tianjiaduohuifuguanjianzi", "tjdhfgjz")
            description = "添加一条关键字回复规则，其回复为多个回复"
            usage = "格式：/keywordAddMultipleReply <类型> <关键字> <以分隔符分隔开的多条回复> <(可选的)回复分隔符> " +
                    "<(可选的)以分号分隔的群号列表，此条规则的开启群> <(可选的)冲突模式>\n" +
                    "请不要输入尖括号，尖括号只是为了让帮助中的参数更明显。\n" +
                    "keywordAddMultipleReply 命令的别名：“tianjiaduohuifuguanjianzi”、“添加多回复关键字”、“tjdhfgjz”\n" +
                    "与keywordAdd命令相同的参数（类型、关键字等）的说明请参见keywordAdd。\n" +
                    "本命令允许一次输入多条回复，这些回复需要被给定的分隔符分开。分隔符需要是一个字符。默认分隔符为*。\n" +
                    "请注意本命令依旧不允许在回复间包含空格。"
            onCommand {
                if (it.count() !in 3..6)
                    return@onCommand false
                //读取用户所提供的参数
                val type = it[0]
                val keyword = it[1]
                val repliesString = it[2]
                var separator = "*"
                var groupsString = ""
                var conflictModeString = "merge"


                fun judgeParameter4() {
                    if (it[3].count() == 1) {
                        separator = it[3]
                    } else {
                        when (it[3]) {
                            in possibleConflictModes -> {
                                conflictModeString = it[3]
                            }
                            else -> groupsString = it[3]
                        }
                    }
                }

                fun judgeParameter5() {
                    when (it[4]) {
                        in possibleConflictModes -> {
                            conflictModeString = it[4]
                        }
                        else -> groupsString = it[4]
                    }
                }
                when (it.count()) {
                    4 -> judgeParameter4()
                    5 -> {
                        judgeParameter4()
                        judgeParameter5()
                    }
                    6 -> {
                        separator = it[3]
                        if (separator.count() > 1)
                            return@onCommand false
                        groupsString = it[4]
                        conflictModeString = it[5]
                    }
                }

                val replies = repliesString.split(separator).toMutableList()

                return@onCommand doAdd(type, groupsString, conflictModeString, keyword, replies)
            }
        }

        registerCommand {
            name = "keywordModify"
            alias = listOf("修改关键字", "xiugaiguanjianzi", "xggjz")
            description = "对关键字规则进行修改"
            usage = "格式：/keywordModify <(可选的)操作> <规则序号> <被修改参数> <新值> <(可选的)分隔符>\n" +
                    "请不要输入尖括号，尖括号只是为了让帮助中的参数更明显。\n" +
                    "操作有：cover（覆盖）【默认值】、append（追加）、remove（删除）、clear（清空）。" +
                    "append和remove只对列表形式的参数有效，clear 只对开启群列表有效，无效情况下将视为 cover。\n" +
                    "序号请参照keywordList输出的序号。\n" +
                    "被修改参数可能值为：type（类型）、keyword（关键字）、replies（回复）、groups（开启群）。\n" +
                    "分隔符不指定时默认为*。分隔符只能为一个字符。"
            onCommand {
                if (it.count() !in 3..5) {
                    return@onCommand false
                }
                var action = "cover"
                var id by Delegates.notNull<Int>()
                var param: String = ""
                var value: String = ""
                var separator = "*"

                val possibleActions = listOf("cover", "覆盖", "append", "追加", "remove", "删除", "clear", "清空")
                val possibleParameters =
                    listOf("type", "类型", "keyword", "关键字", "replies", "回复", "groups", "开启群")

                //填入参数
                when (it.count()) {
                    3 -> {
                        id = it[0].toIntOrNull() ?: -1
                        param = it[1]
                        value = it[2]
                    }
                    4 -> {

                        when (it[0]) {
                            in possibleActions -> {
                                action = it[0]
                            }
                            else -> id = it[0].toIntOrNull() ?: return@onCommand false
                        }

                        when (it[1]) {
                            in possibleActions -> {
                                action = it[1]
                            }
                            in possibleParameters -> {
                                param = it[1]
                            }
                            else -> return@onCommand false
                        }
                        when (it[2]) {
                            in possibleParameters -> {
                                param = it[2]
                            }
                            else -> value = it[2]
                        }
                        if (value.isEmpty()) {
                            value = it[4]
                        } else {
                            separator = it[4]
                            if (separator.count() > 1)
                                return@onCommand false
                        }
                    }
                    5 -> {
                        action = it[0]
                        if (action !in possibleActions)
                            return@onCommand false
                        id = it[1].toIntOrNull() ?: return@onCommand false
                        param = it[2]
                        if (param !in possibleParameters)
                            return@onCommand false
                        value = it[3]
                        separator = it[4]
                        if (separator.count() > 1)
                            return@onCommand false
                    }
                }

                //处理操作别名
                action = when (action){
                    "cover", "覆盖" -> "cover"
                    "append", "追加" -> "append"
                    "remove", "删除" -> "remove"
                    "clear", "清空" -> "clear"
                    else -> return@onCommand false
                }
                //处理参数别名
                param = when (param){
                    "type", "类型" -> "type"
                    "keyword", "关键字" -> "keyword"
                    "replies", "回复" -> "replies"
                    "groups", "开启群" -> "groups"
                    else -> return@onCommand false
                }

                KeywordRuleModifyWorkerFactory.get(action).modify(keywordRules, id, param, value, separator)

                sendMessage("已经对关键字做出修改。\n" +
                        "$id| ${keywordRules[id].toStringHumanFriendly()}")
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
            alias = listOf("删除关键字", "shanchuguanjianzi", "scgjz")
            description = "删除一条关键字回复规则"
            usage = "格式：/keywordRemove <规则序号 或者 all>\n" +
                    "请不要输入尖括号，尖括号只是为了让帮助中的参数更明显。\n" +
                    "keywordRemove 有“删除关键字”、“shanchuguanjianzi”、“scgjz”几个别名。\n" +
                    "序号请参照keywordList输出的序号。请注意删除操作可能会对序号顺序产生改变。\n" +
                    "如果输入的是all，那么所有规则都会被删除。"
            onCommand {
                if (it.count() != 1)
                    return@onCommand false

                if (it[0] == "all") {
                    sendMessage("规则列表已被清空。以下是被删除的规则：\n${keywordRules.mapIndexed { i, rule ->
                        "${i + 1}| ${rule.toStringHumanFriendly()}"
                    }.joinToString("\n")}")
                    keywordRules.clear()
                    return@onCommand true
                }

                val id = (it[0].toIntOrNull() ?: 0) - 1
                if (id == -1 && id !in 0..keywordRules.count())
                    return@onCommand false
                sendMessage("该规则已经被删除：\n${keywordRules.removeAt(id).toStringHumanFriendly()}")
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
                KeywordReplyConfig.saveConfig()
                sendMessage("关键词配置保存完成。")
                return@onCommand true
            }
        }

        registerCommand {
            name = "keywordLoad"
            alias = listOf("读取关键字", "duquguanjianzi", "dqgjz")
            description = "触发一次关键字配置读取"
            usage = "为了方便开发·维护而设的命令。可以使用该命令要求插件读取配置文件中的关键字配置。\n" +
                    "keywordLoad 有“读取关键字”、“duquguanjianzi”、“dqgjz”几个别名。" +
                    "请注意，使用此命令会导致没有保存的关键字配置丢失。"
            onCommand {
                if (it.count() != 0)
                    return@onCommand false
                KeywordReplyConfig.loadConfig()
                return@onCommand true
            }
        }
    }

    private suspend fun CommandSender.doAdd(
        type: String,
        groupsString: String,
        conflictModeString: String,
        keyword: String,
        replies: MutableList<String>
    ): Boolean {
        var type1 = type
        lateinit var addWorker: KeywordRuleAddWorker
        //处理类型别名
        when (type1) {
            "exact", "完全匹配", "完全" -> type1 = "exact"
            "vague", "模糊匹配", "模糊" -> type1 = "vague"
            "regex", "正则匹配", "正则" -> type1 = "regex"
            else -> return false
        }
        //处理群号
        val groups = groupsString.split(";").map { it.toLongOrNull() ?: 0 }.filter { it != 0L }
        if (groupsString.isNotEmpty() && groups.isEmpty())
            return false
        //处理冲突模式
        addWorker = when (conflictModeString) {
            "merge", "合并" -> MergeKeywordRuleAddWorker(keywordRules)
            "cover", "覆盖" -> CoverKeywordRuleAddWorker(keywordRules)
            "keep", "保持" -> KeepKeywordRuleAddWorker(keywordRules)
            "add", "追加" -> AddKeywordRuleAddWorker(keywordRules)
            else -> return false
        }
        try {
            val logMessage = addWorker.add(type1, keyword, replies, groups)
            sendMessage(logMessage)
            logger.info(logMessage)
        } catch (e: RuleTypeConflictException) {
            sendMessage("您输入的规则和现有的相同关键字规则的匹配模式不同，规则未做更改。")
        }
        return true
    }

    override fun onDisable() {
        super.onDisable()
        KeywordReplyConfig.saveConfig()
    }
}