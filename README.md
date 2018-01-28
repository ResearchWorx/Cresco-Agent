Cresco-Agent
=========================

### Status
[![Build Status](http://128.163.188.129:9998/buildStatus/icon?job=Cresco-Agent)](http://128.163.188.129:9998/job/Cresco-Agent/)

---
### Install

1. [Build](#build-section) or [download](http://128.163.188.129:9998/job/Cresco-Agent/lastSuccessfulBuild/com.researchworx.cresco$cresco-agent/) the latest Cresco-Agent and place it in the _cresco_ directory. 
1. [Build](https://github.com/ResearchWorx/Cresco-Agent-Controller-Plugin) or [download](http://128.163.188.129:9998/job/Cresco-Agent-Controller-Plugin/lastStableBuild/com.researchworx.cresco$cresco-agent-controller-plugin/) the latest Cresco-Agent-Controller and place it in the _cresco_/_plugin_ subdirectory.
1. Create a [agent configuration file](#agent-config-section) or modify Cresco-Agent-Plugins.ini.sample
1. Create a [plugin configuration file](https://github.com/ResearchWorx/Cresco-Agent-Controller-Plugin) or modify Cresco-Agent.ini.sample

5. Execute: java -jar  _cresco-agent-\<version\>.jar_ -f _\<location of configuration file\>_

---

### <a name="build-section"></a>Build

1. Confirm you have a working [Java Development Environment](https://www.java.com/en/download/faq/develop.xml) (JDK) 8 or greater.  [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [OpenJDK](http://openjdk.java.net/) are known to work. 
1. Confirm you have a working [Apache Maven 3](https://maven.apache.org) environment.
1. ```git clone https://github.com/ResearchWorx/Cresco-Agent.git```
1. ```cd Cresco-Agent```
1. ```mvn clean package```
1. ```cp target/cresco-agent-[version].jar [cresco directory]```

### <a name="agent-config-section"></a>Agent Configuration

#### General Section
|  pluginpath |   |
|---|---|
| required  | True  |
| type  | String  |
| example  | /opt/cresco/plugins  |
| desc  |  Absolute path of the plugins directory |

| logpath  |   |
|---|---|
| required  | True  |
| type  | String  |
| example  | /opt/cresco/logs  |
| desc  |  Absolute path of the logs directory |

| plugin_config_file  |   |
|---|---|
| required  | True  |
| type  | String  |
| example  | /opt/cresco/cresco-agent-plugins.ini  |
| desc  |  Absolute path of the plugin configuration file |

| startupdelay  |   |
|---|---|
| required  | False  |
| type  | Long  |
| default  | 0  |
| desc  |  Delay before the controller plugin is loaded in milliseconds.  Used in containers to give network devices time to be created/discovered. |

| watchdogtimer  |   |
|---|---|
| required  | False  |
| type  | Long  |
| default  | 5000  |
| desc  |  Time in milliseconds between watchdog messages to regional controller. |

|  location |   |
|---|---|
| required  | False  |
| type  | String  |
| example  | Main/Elm |
| environment variable | CRESCO_LOCATION|
| desc  |  Node descriptor |

|  platform |   |
|---|---|
| required  | False  |
| type  | String  |
| example  | OpenStack |
| environment variable | CRESCO_PLATFORM|
| desc  |  Node descriptor |

|  environment |   |
|---|---|
| required  | False  |
| type  | String  |
| example  | Metal |
| environment variable | CRESCO_ENVIRONMENT|
| desc  |  Node descriptor |


---






###License

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

