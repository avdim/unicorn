package krasa.frameswitcher.networking.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author Vojtech Krasa
 */
public abstract class GeneralMessage implements Serializable {
	protected UUID uuid;
	protected int version = 1;

	public GeneralMessage(UUID uuid) {
		this.uuid = uuid;
	}

	public int getVersion() {
		return version;
	}

	public UUID getUuid() {
		return uuid;
	}

	@Override
	public String toString() {
		return "GeneralMessage toString()";
	}
}
