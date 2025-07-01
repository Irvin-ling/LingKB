# LingKB
Building an Enterprise Knowledge Base in Private Domain

## 项目简介  
1. 请从 [https://file.hankcs.com/hanlp/data-for-1.7.5.zip](https://file.hankcs.com/hanlp/data-for-1.7.5.zip) 
   下载 HanLP 的数据包，解压后得到包含 `dictionary`、`model` 等目录的文件夹。
2. 将解压后的 `data` 文件夹存放至合适路径（例如项目资源目录或本地指定文件夹）。
3. 打开 `hanlp.properties` 配置文件（通常位于项目的 `src/main/resources` 目录），找到 `root` 配置项，
   根据 `data` 文件夹的实际路径进行设置：
   ```properties
   # 示例1：若 data 文件夹在项目资源目录下
   root=/opt/HanLP
   # 示例2：若 data 文件夹在D盘根目录
   root=D:/HanLP
4. 可从 [https://github.com/hankcs/hanlp/wiki/word2vec](https://pan.baidu.com/s/1qYFozrY)
    下载中文 word2vec 的预训练模型，解压后放置到 `model` 目录下
5. 可从 [https://nlp.stanford.edu/projects/glove/](https://nlp.stanford.edu/data/glove.6B.zip)
    下载英文向量的预训练模型，解压后放置到 `model` 目录下

## 文档更新计划  
- **首个版本发布**：2025-07-31  
- **包含内容**：项目简单介绍、简述技术架构、功能清单等  

## 临时说明  
若需了解项目细节、反馈问题或加入项目的开发，可通过以下方式联系：  
- 邮箱：`zhengh@dtdream.com`
- GitHub Issues：[仓库链接](https://github.com/ShiPotian89/LingKB/issues)

## 代码部分内容释义
| 内容                | 说明                          |
|---------------------|------------------------------|
| @CodeHint           | 关键的代码                    |

## 致谢  
感谢你的关注与支持，期待项目后续为你提供更完整的工具能力！

## todoList
| 模块                | 类名                          | TODO                |
|---------------------|-------------------------------|---------------------|
| data.parser         | `tika`                        | 通俗解析逻辑         |
| data.parser         | `ConfluenceTreeParser`        | 图片解析            |
| data.parser         | `WebUrlParser`                | 图片解析            |
| data.parser         | `PdfParser`                   | 中文解析、图片解析   |
| data.parser         | `PptxParser`                  | 图片解析            |
| data.parser         | `WordParser`                  | 图片解析            |
| data.parser         | `***`                         | 字符串格式统一       |
| data.clean          | `***`                         | 各清洗类的使用       |
| data.clean          | `***`                         | 质量控制            |
| data.clean          | `***`                         | 合规与安全处理       |
