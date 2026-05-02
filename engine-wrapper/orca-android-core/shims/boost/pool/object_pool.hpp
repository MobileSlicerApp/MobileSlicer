#ifndef ORCA_ANDROID_CORE_BOOST_POOL_OBJECT_POOL_HPP
#define ORCA_ANDROID_CORE_BOOST_POOL_OBJECT_POOL_HPP

#include <utility>
#include <vector>

namespace boost {

template <typename T>
class object_pool {
public:
    object_pool() = default;
    ~object_pool()
    {
        for (T* ptr : allocated_) {
            delete ptr;
        }
    }

    template <typename... Args>
    T* construct(Args&&... args)
    {
        T* value = new T(std::forward<Args>(args)...);
        allocated_.push_back(value);
        return value;
    }

private:
    std::vector<T*> allocated_;
};

} // namespace boost

#endif
