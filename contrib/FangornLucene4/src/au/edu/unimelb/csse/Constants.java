package au.edu.unimelb.csse;

import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.PhysicalPayloadFormatAware;

public interface Constants {

	String FIELD_NAME = "s";

	PhysicalPayloadFormatAware DEFAULT_PAYLOAD_FORMAT = new BytePacking(4);
}
