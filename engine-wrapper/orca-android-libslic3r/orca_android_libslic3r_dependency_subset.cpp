#include <cstddef>
#include <numeric>
#include <string>
#include <type_traits>
#include <vector>

#include <boost/filesystem/path.hpp>
#include <boost/system/error_code.hpp>

#include <CGAL/Exact_predicates_exact_constructions_kernel.h>
#include <CGAL/enum.h>

#include <tbb/task_arena.h>

#include "libslic3r/Execution/ExecutionTBB.hpp"
#include "libslic3r/Execution/ExecutionSeq.hpp"

namespace orca_android_libslic3r {

int dependency_backed_subset_probe()
{
    using ExactKernel = CGAL::Exact_predicates_exact_constructions_kernel;

    static_assert(std::is_same_v<decltype(Slic3r::ex_tbb), const Slic3r::ExecutionTBB>);
    static_assert(std::is_same_v<decltype(Slic3r::ex_seq), const Slic3r::ExecutionSeq>);

    boost::filesystem::path model_path("sample.stl");
    boost::system::error_code error;
    (void)error;

    tbb::task_arena arena(1);
    std::vector<int> values{1, 2, 3, 4};
    int sequential_sum = 0;
    Slic3r::execution::for_each(Slic3r::ex_seq, values.begin(), values.end(), [&](int value) {
        sequential_sum += value;
    });
    const int reduced_sum = Slic3r::execution::reduce(
        Slic3r::ex_tbb,
        values.begin(),
        values.end(),
        0,
        [](int lhs, int rhs) { return lhs + rhs; });

    const ExactKernel::Point_2 a(0, 0);
    const ExactKernel::Point_2 b(1, 0);
    const ExactKernel::Point_2 c(0, 1);

    const bool left_turn = CGAL::orientation(a, b, c) == CGAL::LEFT_TURN;
    const std::size_t extension_size = model_path.extension().string().size();
    return static_cast<int>(
        extension_size +
        arena.max_concurrency() +
        (left_turn ? 1U : 0U) +
        static_cast<std::size_t>(sequential_sum + reduced_sum));
}

} // namespace orca_android_libslic3r
