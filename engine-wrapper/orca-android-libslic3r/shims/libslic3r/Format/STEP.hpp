#ifndef slic3r_Format_STEP_hpp_
#define slic3r_Format_STEP_hpp_

#include <functional>
#include <string>

namespace Slic3r {

class Model;
class TriangleMesh;
class ModelObject;
class Step;

const int LOAD_STEP_STAGE_READ_FILE = 0;
const int LOAD_STEP_STAGE_GET_SOLID = 1;
const int LOAD_STEP_STAGE_GET_MESH = 2;
const int LOAD_STEP_STAGE_NUM = 3;
const int LOAD_STEP_STAGE_UNIT_NUM = 5;

typedef std::function<void(int load_stage, int current, int total, bool& cancel)> ImportStepProgressFn;
typedef std::function<void(bool isUtf8)> StepIsUtf8Fn;

bool load_step(const char *path, Model *model, bool& is_cancel,
               double linear_defletion = 0.003, double angle_defletion = 0.5,
               bool isSplitCompound = false, ImportStepProgressFn proFn = nullptr,
               StepIsUtf8Fn isUtf8Fn = nullptr, long& mesh_face_num = *(new long(-1)));

class Step
{
public:
    enum class Step_Status {
        LOAD_SUCCESS,
        LOAD_ERROR,
        CANCEL,
        MESH_SUCCESS,
        MESH_ERROR
    };

    Step(std::string path, ImportStepProgressFn stepFn = nullptr, StepIsUtf8Fn isUtf8Fn = nullptr);
    ~Step();
    Step_Status load();
    unsigned int get_triangle_num(double linear_defletion, double angle_defletion);
    unsigned int get_triangle_num_tbb(double linear_defletion, double angle_defletion);
    void clean_mesh_data();
    Step_Status mesh(Model* model, bool& is_cancel, bool isSplitCompound,
                     double linear_defletion = 0.003, double angle_defletion = 0.5);
    void update_process(int load_stage, int current, int total, bool& cancel);
};

} // namespace Slic3r

#endif
