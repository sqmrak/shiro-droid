#include "rootd_firewall.h"
#include "test_util.h"

typedef struct { unsigned calls, fail_at; char verbs[32]; } fixture;
static int execute(void *context, const char *const *argv) {
    fixture *f = context;
    unsigned n = f->calls++;
    size_t i;
    char verb = '?';
    for (i = 1; argv[i]; ++i) if (argv[i][0] == '-' && argv[i][1] && !argv[i][2] && strchr("NAXDI", argv[i][1])) { verb = argv[i][1]; break; }
    if (n < sizeof(f->verbs)) f->verbs[n] = verb;
    return n == f->fail_at ? -1 : 0;
}
int main(void) {
    unsigned i;
    for (i = 0; i < 8; ++i) {
        rootd_firewall state = {0};
        fixture f = {0}; f.fail_at = i;
        CHECK_EQ_INT(rootd_firewall_start(&state, "1.1.1.1", execute, &f), -1);
        CHECK_EQ_INT(state.applied, 0);
        CHECK_EQ_INT(f.calls, i * 2 + 1);
    }
    {
        rootd_firewall state = {0}; fixture f = {0}; f.fail_at = 99;
        CHECK_EQ_INT(rootd_firewall_start(&state, "1.1.1.1", execute, &f), 0);
        CHECK_EQ_INT(state.applied, 255);
        CHECK_EQ_INT(rootd_firewall_stop(&state, execute, &f), 0);
        CHECK_EQ_INT(state.applied, 0);
        CHECK_EQ_INT(f.calls, 16);
        CHECK_EQ_INT(rootd_firewall_start(&state, "bad", execute, &f), -1);
    }
    {
        rootd_firewall state = {0}; fixture f = {0}; f.fail_at = 99;
        CHECK_EQ_INT(rootd_firewall_start(&state, "1.1.1.1", execute, &f), 0);
        f.fail_at = f.calls;
        CHECK_EQ_INT(rootd_firewall_stop(&state, execute, &f), -1);
        CHECK(state.applied != 0);
        f.fail_at = 99;
        CHECK_EQ_INT(rootd_firewall_stop(&state, execute, &f), 0);
        CHECK_EQ_INT(state.applied, 0);
    }
    return test_finish("root_firewall");
}
