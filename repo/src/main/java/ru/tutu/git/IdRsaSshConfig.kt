package ru.tutu.git

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import org.eclipse.jgit.transport.JschConfigSessionFactory
import org.eclipse.jgit.transport.OpenSshConfig

class IdRsaSshConfig(val idRsaPassphrase: String? = null) : TutuGit.Config {
  override val configSessionFactory = object : JschConfigSessionFactory() {

//    val jsch: JSch = JSch().apply {
//      addIdentity("~/.ssh/id_rsa")
//    }

    init {
//      configureJSch(jsch)
    }

    override fun configure(host: OpenSshConfig.Host, session: Session) {
//      session.setConfig("StrictHostKeyChecking", "false")
//
//
//      session.userInfo = object : UserInfo {
//        override fun getPassphrase() = idRsaPassphrase
//        override fun getPassword() = null
//        override fun promptPassword(message: String) = false
//        override fun promptPassphrase(message: String) = true
//        override fun promptYesNo(message: String) = false
//        override fun showMessage(message: String) = Unit
//      }
    }
  }
}
