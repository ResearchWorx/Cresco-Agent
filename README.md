#  <span style="color:red">This Project Has *Moved* ! </span>

Please see [https://github.com/CrescoEdge/quickstart](https://github.com/CrescoEdge/quickstart) for new Cresco repositories.


---


Cresco-Agent
=========================

### Status
[![Build Status](http://128.163.188.129:9998/buildStatus/icon?job=Cresco-Agent)](http://128.163.188.129:9998/job/Cresco-Agent/)

---
### Install

1. Download/update/confirm a [Java Runtime Environment](http://www.oracle.com/technetwork/java/javase/overview/index.html) (JRE, Java Runtime) 1.6 or greater.
2. Download the [Latest Build](http://128.163.188.129:9998/job/Cresco-Agent/lastSuccessfulBuild/com.researchworx.cresco$cresco-agent/) of the Cresco-Agent. 
3. Copy _Cresco-Agent.ini.sample_ to _Cresco-Agent.ini_
4. Modify _Cresco-Agent.ini_ for your enviorment.  Make sure to configure the controll channel.
5. Execute: java -jar  _cresco-agent-\<version\>.jar_ -f _\<location of configuration file\>_

---

### Usage

1. SSH to the hostname of the controller server.
2. Login using account: _admin_ and password: _admin_ **(actually any account will work as long as username matches password, this must be corrected soon)**
3. Follow shell instructions.
4. Agent commands can be found under the name of the shell: _\> \<agent name\> \<command\>_

---

###License

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

###Project lead

Cody Bumgardner (@codybum)
