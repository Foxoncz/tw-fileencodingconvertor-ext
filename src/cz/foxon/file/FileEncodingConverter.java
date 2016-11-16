package cz.foxon.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Set;

import com.thingworx.common.RESTAPIConstants;
import com.thingworx.common.exceptions.InvalidRequestException;
import com.thingworx.entities.utils.ThingUtilities;
import com.thingworx.metadata.annotations.ThingworxServiceDefinition;
import com.thingworx.metadata.annotations.ThingworxServiceParameter;
import com.thingworx.metadata.annotations.ThingworxServiceResult;
import com.thingworx.resources.Resource;
import com.thingworx.things.Thing;
import com.thingworx.things.repository.FileRepositoryThing;

public class FileEncodingConverter extends Resource {
	private static final long serialVersionUID = -72415315242961261L;

	static class FileEncoder {
		private FileEncoder() {
		}

		public static void encodeFile(InputStreamReader is, OutputStreamWriter os) throws IOException {
			try (BufferedReader br = new BufferedReader(is); BufferedWriter bw = new BufferedWriter(os);) {
				char[] buffer = new char[16384];
				int read;
				while ((read = br.read(buffer)) != -1)
					bw.write(buffer, 0, read);
			}
		}

		public static Set<String> getAvailableEncodings() {
			return Charset.availableCharsets().keySet();
		}
	}

	public FileEncodingConverter() {
	}

	@ThingworxServiceDefinition(name = "ConvertFile", description = "Converts file in given repository from one encoding to another.", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "", baseType = "NOTHING", aspects = {})
	public void ConvertFile(
			@ThingworxServiceParameter(name = "fileRepository", description = "Name of the file repository.", baseType = "STRING", aspects = {
					"isRequired:true" }) String fileRepository,
			@ThingworxServiceParameter(name = "inputFilePath", description = "Path to file that should be read.", baseType = "STRING", aspects = {
					"isRequired:true" }) String inputFilePath,
			@ThingworxServiceParameter(name = "inputEncoding", description = "Encoding in which the given file should be read, for options please see GetAvailableEncodings.", baseType = "STRING", aspects = {
					"isRequired:true" }) String inputEncoding,
			@ThingworxServiceParameter(name = "outputFilePath", description = "Path to file that should be written.", baseType = "STRING", aspects = {
					"isRequired:true" }) String outputFilePath,
			@ThingworxServiceParameter(name = "outputEncoding", description = "Encoding in which the given file should be written, for options please see GetAvailableEncodings.", baseType = "STRING", aspects = {
					"isRequired:true" }) String outputEncoding)
			throws Exception {
		if ((String) fileRepository == null || ((String) fileRepository.trim()).isEmpty()) {
			throw new InvalidRequestException("File Repository Must Be Specified",
					RESTAPIConstants.StatusCode.STATUS_NOT_ACCEPTABLE);
		}
		Thing thing = ThingUtilities.findThing((String) fileRepository);
		if (thing == null) {
			throw new InvalidRequestException("File Repository [" + fileRepository + "] Does Not Exist",
					RESTAPIConstants.StatusCode.STATUS_NOT_FOUND);
		}
		if (!(thing instanceof FileRepositoryThing)) {
			throw new InvalidRequestException("Thing [" + fileRepository + "] Is Not A File Repository",
					RESTAPIConstants.StatusCode.STATUS_NOT_FOUND);
		}
		FileRepositoryThing repo = (FileRepositoryThing) thing;

		Charset iEnc, oEnc;
		try {
			iEnc = Charset.forName(inputEncoding);
		} catch (Exception e) {
			throw new InvalidRequestException("Bad input encoding string was specified.",
					RESTAPIConstants.StatusCode.STATUS_NOT_ACCEPTABLE);
		}
		try {
			oEnc = Charset.forName(outputEncoding);
		} catch (Exception e) {
			throw new InvalidRequestException("Bad output encoding string was specified.",
					RESTAPIConstants.StatusCode.STATUS_NOT_ACCEPTABLE);
		}
		try {
			InputStreamReader is = new InputStreamReader(repo.openFileForRead(inputFilePath), iEnc);
			OutputStreamWriter os = new OutputStreamWriter(
					repo.openFileForWrite(outputFilePath, FileRepositoryThing.FileMode.WRITE), oEnc);
			FileEncoder.encodeFile(is, os);
		} catch (IOException ioe) {
			throw new IOException(ioe);
		} catch (Exception eOpen) {
			throw new InvalidRequestException(
					"Unable To Open [" + inputFilePath + "] in [" + fileRepository + "] : " + eOpen.getMessage(),
					RESTAPIConstants.StatusCode.STATUS_NOT_FOUND);
		}
	}

	@ThingworxServiceDefinition(name = "GetAvailableEncodings", description = "List all possible encodings to choose from.", category = "", isAllowOverride = false, aspects = {
			"isAsync:false" })
	@ThingworxServiceResult(name = "Result", description = "", baseType = "STRING", aspects = {})
	public String GetAvailableEncodings() {
		return FileEncoder.getAvailableEncodings().toString();
	}

}
