sealed class IdeaVersion {
  abstract val sandboxDir: String?

  class Community(val version: String) : IdeaVersion() {
    override val sandboxDir: String = tmpDir()//"/tmp/idea_sandbox"
  }

  class Local(val localPath: String) : IdeaVersion() {
    override val sandboxDir: String = "/tmp/local_idea_ultimate"
  }

}
