#include "Security.h"
#include <iomanip>
#include <sstream>

// generate sha256 hash
void Security::compute_sha256(const unsigned char* data, size_t len, unsigned char* out)
{
    EVP_MD_CTX* ctx = EVP_MD_CTX_new();
    if (ctx == NULL) {
        return;
    }
    if (EVP_DigestInit_ex(ctx, EVP_sha256(), NULL) != 1) {
        EVP_MD_CTX_free(ctx);
        return;
    }
    if (EVP_DigestUpdate(ctx, data, len) != 1) {
        EVP_MD_CTX_free(ctx);
        return;
    }
    if (EVP_DigestFinal_ex(ctx, out, NULL) != 1) {
        EVP_MD_CTX_free(ctx);
        return;
    }
    EVP_MD_CTX_free(ctx);
}

// helper to generate sha256 hash
std::string Security::hashSHA256(const std::string& password)
{
   unsigned char outBuffer[EVP_MAX_MD_SIZE];
    compute_sha256(reinterpret_cast<const unsigned char*>(password.data()), password.size(), outBuffer);
    std::ostringstream hexStream;
    for (int i = 0; i < 32; ++i) // 32 bytes for SHA-256
    { 
        hexStream << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(outBuffer[i]);
    }
    return hexStream.str();
}

// Function to convert a string to a byte array (hex to binary)
std::vector<unsigned char> Security::hexStringToBytes(const std::string& hex)
{
    std::vector<unsigned char> bytes;
    for (size_t i = 0; i < hex.length(); i += 2)
    {
        std::string byteString = hex.substr(i, 2);
        unsigned char byte = static_cast<unsigned char>(strtol(byteString.c_str(), nullptr, 16));
        bytes.push_back(byte);
    }
    return bytes;
}

// use slow hashing to generate hash.
void Security::generateSlowHash(const std::string& password, unsigned char* salt, int salt_len, int iterations, unsigned char* out, int out_len)
{
    // Use SHA-256 for HMAC
    const EVP_MD* digest = EVP_sha256();

    // Generate the hash
    if (!PKCS5_PBKDF2_HMAC(password.c_str(), password.length(), salt, salt_len, iterations, digest, out_len, out))
    {
        std::cerr << "Error in PKCS5_PBKDF2_HMAC" << std::endl;
    }
}

// helper to use slow hashing to generate hash
std::string Security::slowHash(const std::string& password, const std::string& saltHex)
{
    std::vector<unsigned char> salt = hexStringToBytes(saltHex);
    unsigned char hash[Security::hashLength];

    generateSlowHash(password, salt.data(), salt.size(), Security::iterations, hash, Security::hashLength);

    std::ostringstream oss;
    for (int i = 0; i < Security::hashLength; ++i) {
        oss << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(hash[i]);
    }
    return oss.str();
}

// generate salt - random string
std::string Security::generateSalt()
{
    std::vector<unsigned char> salt(Security::saltLength);
    if (!RAND_bytes(salt.data(), Security::saltLength)) {
        std::cerr << "Error generating random salt" << std::endl;
        return "";
    }

    std::ostringstream oss;
    for (int i = 0; i < Security::saltLength; ++i) {
        oss << std::hex << std::setw(2) << std::setfill('0') << static_cast<int>(salt[i]);
    }
    return oss.str();
}

// create base64 encode
std::string Security::base64_encode(const unsigned char* input, size_t length)
{
    BIO* bio, * b64;
    BUF_MEM* bufferPtr;

    // Create a Base64 BIO
    b64 = BIO_new(BIO_f_base64());
    BIO_set_flags(b64, BIO_FLAGS_BASE64_NO_NL);
    bio = BIO_new(BIO_s_mem());
    bio = BIO_push(b64, bio);
    BIO_write(bio, input, length);

    BIO_flush(bio);
    BIO_get_mem_ptr(bio, &bufferPtr);

    std::string encoded(bufferPtr->data, bufferPtr->length);

    BIO_free_all(bio);
    return encoded;
}
