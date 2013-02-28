package au.edu.unimelb.csse;

import au.edu.unimelb.csse.paypack.BytePacking;
import au.edu.unimelb.csse.paypack.PayloadFormatAware;

public interface Constants {

	String FIELD_NAME = "s";

	PayloadFormatAware PAYLOAD_FORMAT = new BytePacking();
}
