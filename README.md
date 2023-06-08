# SkipAds

android skip ads

# How to use

1. Get accessibility permissions.  
   Each brand of mobile phone entrance has a slight difference, please find yourself.
2. Load the configuration file.  
   You can put the file(*.json) in the /sdcard root directory.

# Rule config

| Name         | Type      | Description                                        |
| :------------- | :---------- | :--------------------------------------------------- |
| package      | String    | `*Required*` package name                          |
| rule         | String    | `*Required*` value:`default`, `custom`             |
| custom_rules | JSONArray | when rule is`custom` need this, see `custom rules` |

`custom_rules`

| Name    | Type   | Description                                                              |
| :-------- | :------- | :------------------------------------------------------------------------- |
| keyword | String | `*Optional*` triggered keyword, default value:`跳过`                     |
| class   | String | `*Optional*` the class name of the triggered                             |
| action  | String | `*Optional*` value:`click`, `back`, default value: `click`, see `action` |

`action`

| Name  | Description           |
| :------ | :---------------------- |
| click | click the view        |
| back  | click the back button |

```json
[
{"package": "com.test.app1", "rule": "default"},
{"package": "com.test.app2", "rule": "custom", "custom_rules":[
    {"keyword": "以后再说", "class": "com.test.app2.MainActivity", "action": "click"},
    {"keyword": "马上更新", "action": "back"}
]}
]
```
