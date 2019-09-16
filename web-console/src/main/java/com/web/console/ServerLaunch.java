package com.web.console;

import com.web.common.SystemInfoUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * <p>简要说明...</p>
 *
 * @author JingHe
 * @version 1.0
 * @date 2019/9/16
 */
@SpringBootApplication
public class ServerLaunch {

	public static void main(String[] args) throws IOException {
		FileUtils.writeStringToFile(new File(System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + SystemInfoUtil.COMMAND + ".pid.launch"), String.valueOf(SystemInfoUtil.PID), Charset.defaultCharset());
		SpringApplication.run(ServerLaunch.class, args);
	}
}
