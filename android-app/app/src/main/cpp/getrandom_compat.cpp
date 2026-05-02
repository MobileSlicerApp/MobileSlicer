#include <errno.h>
#include <fcntl.h>
#include <stddef.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <unistd.h>

namespace {

static ssize_t read_fallback_random(void* buf, size_t buflen)
{
    char* out = static_cast<char*>(buf);
    size_t bytes_read = 0u;

    int file = open("/dev/urandom", O_RDONLY | O_CLOEXEC);
    if (file == -1)
    {
        file = open("/dev/random", O_RDONLY | O_CLOEXEC);
        if (file == -1)
            return -1;
    }

    while (bytes_read < buflen)
    {
        const size_t remaining = buflen - bytes_read;
        ssize_t n = read(file, out + bytes_read, remaining);
        if (n < 0)
        {
            const int err = errno;
            if (err == EINTR)
                continue;
            close(file);
            return -1;
        }
        bytes_read += static_cast<size_t>(n);
    }

    close(file);
    return static_cast<ssize_t>(bytes_read);
}

}

extern "C" ssize_t getrandom(void* buf, size_t buflen, unsigned int flags)
{
    (void)flags;

    if (!buf && buflen != 0)
    {
        errno = EFAULT;
        return -1;
    }

    if (buflen == 0)
        return 0;

    size_t bytes_read = 0u;

#if defined(SYS_getrandom)
    while (bytes_read < buflen)
    {
        const size_t remaining = buflen - bytes_read;
        ssize_t n = syscall(SYS_getrandom, static_cast<char*>(buf) + bytes_read, remaining, flags);
        if (n < 0)
        {
            const int err = errno;
            if (err == EINTR)
                continue;
            if (err == ENOSYS)
                break;
            return -1;
        }
        bytes_read += static_cast<size_t>(n);
    }

    if (bytes_read == buflen)
        return static_cast<ssize_t>(bytes_read);
#endif

    const ssize_t fallback_read = read_fallback_random(static_cast<char*>(buf) + bytes_read, buflen - bytes_read);
    if (fallback_read < 0)
        return -1;

    return static_cast<ssize_t>(bytes_read + static_cast<size_t>(fallback_read));
}
