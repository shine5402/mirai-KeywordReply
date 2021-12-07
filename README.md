# KeywordReply for mirai

本仓库是为 [mirai-console](https://github.com/mamoe/mirai-console) 开发的关键字自动回复插件。

## 使用
将本插件的 [release jar](https://github.com/shine5402/mirai-KeywordReply/releases) 放置在 mirai 的 plugins 文件夹下，然后使用 reload 命令要求重载插件或者重启 mirai-console 即可。

本插件会注册以下命令:

- keywordAdd 添加关键词触发规则
- keywordModify 修改关键词触发规则
- keywordRemove 删除关键词触发规则
- keywordList 列出关键词触发规则
- keywordLoad 加载关键词触发规则（维护用）
- keywordSave 保存关键词触发规则（维护用）

这些命令如何使用可以直接查看调用这些功能时对应的[描述](./Parameter Description.txt)。

## 兼容性
本插件的目标与测试 mirai 版本为：core：1.1.3；console：0.5.2。

其他版本的 mirai 和 mirai-console 可能无法正常运作，还请自行测试。
！！本插件应当与现行的mirai**不兼容**，请寻找其他可用替代。！！

## 许可证
本插件以 GNU AGPL 3.0 分发。详见 [LICENSE](LICENSE)。

    KeywordReply for mirai
    Copyright (C) 2020 shine_5402

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.


## TODO

- [ ] 完善readme
- [ ] 回复规则优先级 //这个功能的开发优先级不高
- [ ] 开启·关闭 规则 //这个功能的开发优先级不高
- [ ] keywordList 筛选
