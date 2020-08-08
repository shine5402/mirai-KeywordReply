package top.shine5402

object KeywordReplyConfig {
    private val config = KeywordReply.loadConfig("settings.yml")
    var configVersion
        get() = config["version"].toString().toIntOrNull() ?: 1
        set(value: Int) { config["version"] = value }

    fun loadConfig() {
        upgradeConfig()
        val keywordRulesConfigSection = if (config.exist("rules")) config.getConfigSectionList("rules") else null
        KeywordReply.keywordRules.clear()
        KeywordReply.keywordRules.addAll(keywordRulesConfigSection?.map {
            KeywordRuleFactory.fromConfigSection(it)
        }?.toMutableList() ?: mutableListOf())
    }

    fun saveConfig() {
        configVersion = 2
        config.set("rules", KeywordReply.keywordRules.map { it.toConfigSection()})
        config.save()
    }

    private const val supportedVersion = 2
    private fun upgradeConfig(){
        if (configVersion > supportedVersion){
            KeywordReply.logger.warning("配置文件版本高于此 KeywordReply 支持的版本。程序将继续运作，但很有可能出现错误。\n" +
                    "并且请注意备份当前配置文件，否则在 KeywordReply 退出/被要求保存配置文件时，当前配置文件将丢失。")
        return
        }

        if (configVersion == supportedVersion){
            return
        }

        when (configVersion){
            1 -> {
                val keywordRulesConfigSection = if (config.exist("rules")) config.getConfigSectionList("rules") else listOf()
                    for (ruleConfig in keywordRulesConfigSection){
                        val reply = ruleConfig.getString("reply")
                        ruleConfig["replies"] = listOf(reply)
                        ruleConfig.remove("reply")
                        val matchGroupID = ruleConfig.getLongList("matchGroupID")
                        ruleConfig["groups"] = matchGroupID
                        ruleConfig.remove("matchGroupID")
                    }
            }
        }
    }
}