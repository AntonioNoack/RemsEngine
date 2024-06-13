#include <iostream>
#include <random>
#include <cstdlib>
#include <filesystem>
#include <fstream>

// set this to whatever size your built executable has on your OS
#define JAR_OFFSET 70656

#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#endif

void openBrowser(const std::string& url) {
#if defined(_WIN32) || defined(_WIN64)
	ShellExecuteA(nullptr, "open", url.c_str(), nullptr, nullptr, SW_SHOWNORMAL);
#elif defined(__APPLE__)
	std::string command = "open " + url;
	system(command.c_str());
#elif defined(__linux__)
	std::string command = "xdg-open " + url;
	system(command.c_str());
#else
#error "Unsupported OS"
#endif
}

bool isJavaInstalled() {
	// todo check actual version
    return system("java -version") == 0;
}

int copyFile(std::string& src, std::string& dst, std::streampos pos) {
	std::ifstream input(src, std::ios::binary);
	if (!input.is_open()) {
		std::cerr << "Failed to open src" << std::endl;
		return 1;
	}

	std::ofstream output(dst, std::ios::binary);
	if (!output.is_open()) {
		input.close();
		std::cerr << "Failed to open dst" << std::endl;
		return 2;
	}
	
	input.seekg(pos);
	if (!input) {
		input.close();
		output.close();
		std::cerr << "Failed seekg" << std::endl;
		return 3;
	}

	output << input.rdbuf();
	if (!output) {
		input.close();
		output.close();
		std::cerr << "Failed writing to dst" << std::endl;
		return 4;
	}

	input.close();
	output.close();
	return 0;
}

std::string generateRandomFileName() {
	std::random_device dev;
	std::mt19937 rng(dev());
	std::uniform_int_distribution<std::mt19937::result_type> dist(0, 26*2+10-1);
	std::string result;
	std::string extension = ".jar";
	size_t length = 16;
	result.reserve(length + extension.length());
	for (size_t i = 0; i < length; i++) {
		int rnd = dist(rng);
		char c = rnd < 26 ? 'A' + rnd :
			rnd < 26*2 ? 'a' + rnd - 26 :
			'0' + rnd - 26*2;
		result += c;
	}
	result += extension;
	return result;
}

int main(int argc, char** argv) {
	// check if Java is installed
	if (isJavaInstalled() && argc > 0) {
		// all good, probably
		// if yes, unpack .jar, and run
		std::filesystem::path tmpFolder = std::filesystem::temp_directory_path();
		std::filesystem::path tmpFile = tmpFolder / generateRandomFileName();
		std::string tmpFilePath = tmpFile.string();
		std::string srcFile(argv[0]);
		std::cout << "Copying executable to " << tmpFilePath << std::endl;
		int code = copyFile(srcFile, tmpFilePath, JAR_OFFSET);
		std::cout << "Created file length: " << std::filesystem::file_size(tmpFile) << std::endl;
		if (code == 0) {
			std::string command = "java -jar \"" + tmpFile.string() + "\""; // C++, why :(, why can't I safely execute a command???
			int returnCode = std::system(command.c_str());
			// std::filesystem::remove(tmpFile);
			return returnCode;
		}
		else return code;
	}
	else {
		// if no, redirect user to Java install, or ask them kindly to install it themselves
		std::cerr << "Missing Java, redirecting user to download it" << std::endl;
		openBrowser("https://duckduckgo.com/?q=Install+Java+8");
		return 101;
	}
}