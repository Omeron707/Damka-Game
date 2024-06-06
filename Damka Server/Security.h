#pragma once
#include <iostream>
#include <openssl/sha.h>
#include <openssl/bio.h>
#include <openssl/evp.h>
#include <openssl/buffer.h>
#include <openssl/rand.h>
#include <vector>

class Security
{
public:
	static const int iterations = 100000;
	static const int hashLength = 32;
	static const int saltLength = 16;

	static void compute_sha256(const unsigned char* data, size_t len, unsigned char* out);
	static std::string hashSHA256(const std::string& password);
	static std::vector<unsigned char> hexStringToBytes(const std::string& hex);
	static void generateSlowHash(const std::string& password, unsigned char* salt, int salt_len, int iterations, unsigned char* out, int out_len);
	static std::string slowHash(const std::string& password, const std::string& salt);
	static std::string generateSalt();
	static std::string base64_encode(const unsigned char* input, size_t length);
};