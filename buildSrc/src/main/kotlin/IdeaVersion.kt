sealed class IdeaVersion {
  abstract val type: String?

  class Download(val version: String, override val type: String = "IC") : IdeaVersion()

  class Local(val localPath: String) : IdeaVersion() {
    override val type: String? = null
  }

}
