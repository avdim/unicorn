sealed class IdeaVersion {
  abstract val sandboxDir: String?

  class Community(val version: String) : IdeaVersion() {
    override val sandboxDir: String get() = tmpDir()//"/tmp/idea_sandbox"
  }

  class Local(val localPath: String) : IdeaVersion() {
    override val sandboxDir: String get() = "/tmp/local_idea_ultimate"
  }

}
