sealed class IdeaVersion {
  abstract val sandboxDir: String?
  abstract val type: String?

  class Download(val version: String) : IdeaVersion() {
    override val sandboxDir: String get() = tmpDir()//"/tmp/idea_sandbox"
    override val type: String = DOWNLOAD_IDEA_TYPE
  }

  class Local(val localPath: String) : IdeaVersion() {
    override val sandboxDir: String get() = "/tmp/local_idea_ultimate"
    override val type: String? = null
  }

}
