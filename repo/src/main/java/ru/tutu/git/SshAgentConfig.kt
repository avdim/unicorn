package ru.tutu.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.agentproxy.AgentProxyException
import com.jcraft.jsch.agentproxy.Connector
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository
import com.jcraft.jsch.agentproxy.USocketFactory
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig
import org.eclipse.jgit.util.FS
import ru.tutu.log.TutuLog

class SshAgentConfig : TutuGit.Config {
    override val configSessionFactory = object : JschConfigSessionFactory() {
        override fun configure(hc: OpenSshConfig.Host, session: Session) {
            session.setConfig("StrictHostKeyChecking", "false")
        }

        override fun createDefaultJSch(fs: FS?): JSch {
            //https://gist.github.com/quidryan/5449155
            var con: Connector? = null
            try {
                if (SSHAgentConnector.isConnectorAvailable()) {
                    val usf: USocketFactory = JNAUSocketFactory()// alternative: JUnixDomainSocketFactory()
                    con = SSHAgentConnector(usf)
                }
            } catch (e: AgentProxyException) {
                TutuLog.error(e.message!!)
            }

            val jsch: JSch = super.createDefaultJSch(fs)
            if (con != null) {
                JSch.setConfig("PreferredAuthentications", "publickey")

                val identityRepository = RemoteIdentityRepository(con)
                jsch.identityRepository = identityRepository
            }
            return jsch
        }
    }
}