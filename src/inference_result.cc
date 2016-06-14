#include "inference_result.h"
#include "factor_graph.h"
#include <iostream>

namespace dd {

InferenceResult::InferenceResult(const CompiledFactorGraph &fg,
                                 const CmdParser &opts)
    : fg(fg),
      opts(opts),
      nvars(fg.size.num_variables),
      nweights(fg.size.num_weights),
      ntallies(0),
      multinomial_tallies(NULL),
      agg_means(new double[nvars]),
      agg_nsamples(new double[nvars]),
      assignments_free(new VariableValue[nvars]),
      assignments_evid(new VariableValue[nvars]),
      weight_values(new double[nweights]),
      weights_isfixed(new bool[nweights]) {}

InferenceResult::InferenceResult(const CompiledFactorGraph &fg,
                                 const Weight weights[], const CmdParser &opts)
    : InferenceResult(fg, opts) {
  for (long t = 0; t < nweights; t++) {
    const Weight &weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weights_isfixed[weight.id] = weight.isfixed;
  }

  ntallies = 0;
  for (long t = 0; t < nvars; t++) {
    const Variable &variable = fg.variables[t];
    assignments_free[variable.id] = variable.assignment_free;
    assignments_evid[variable.id] = variable.assignment_evid;
    if (variable.domain_type == DTYPE_MULTINOMIAL) {
      ntallies += variable.cardinality;
    }
  }

  multinomial_tallies.reset(new int[ntallies]);

  clear_variabletally();
}

InferenceResult::InferenceResult(const InferenceResult &other)
    : InferenceResult(other.fg, other.opts) {
  memcpy(assignments_evid.get(), other.assignments_evid.get(),
         sizeof(*assignments_evid.get()) * nvars);
  memcpy(agg_means.get(), other.agg_means.get(),
         sizeof(*agg_means.get()) * nvars);
  memcpy(agg_nsamples.get(), other.agg_nsamples.get(),
         sizeof(*agg_nsamples.get()) * nvars);

  memcpy(weight_values.get(), other.weight_values.get(),
         sizeof(*weight_values.get()) * nweights);
  memcpy(weights_isfixed.get(), other.weights_isfixed.get(),
         sizeof(*weights_isfixed.get()) * nweights);

  ntallies = other.ntallies;
  multinomial_tallies.reset(new int[ntallies]);
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = other.multinomial_tallies[i];
  }
}

void InferenceResult::clear_variabletally() {
  for (long i = 0; i < nvars; i++) {
    agg_means[i] = 0.0;
    agg_nsamples[i] = 0.0;
  }
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = 0;
  }
}

void InferenceResult::show_histogram(std::ostream &os) {
  // show a histogram of inference results
  os << "INFERENCE CALIBRATION (QUERY BINS):" << std::endl;
  std::vector<int> abc;
  for (int i = 0; i <= 10; ++i) {
    abc.push_back(0);
  }
  int bad = 0;
  for (long j = 0; j < nvars; ++j) {
    const Variable &variable = fg.variables[j];
    if (!opts.should_sample_evidence && variable.is_evid) {
      continue;
    }
    int bin = (int)(agg_means[variable.id] / agg_nsamples[variable.id] * 10);
    if (bin >= 0 && bin <= 10) {
      ++abc[bin];
    } else {
      ++bad;
    }
  }
  abc[9] += abc[10];
  for (int i = 0; i < 10; ++i) {
    os << "PROB BIN 0." << i << "~0." << (i + 1) << "  -->  # " << abc[i]
       << std::endl;
  }
}

void InferenceResult::dump_marginals(std::ostream &text_output) {
  for (long j = 0; j < nvars; ++j) {
    const Variable &variable = fg.variables[j];
    if (variable.is_evid && !opts.should_sample_evidence) {
      continue;
    }

    switch (variable.domain_type) {
      case DTYPE_BOOLEAN: {
        text_output << variable.id << " " << 1 << " "
                    << (agg_means[variable.id] / agg_nsamples[variable.id])
                    << std::endl;
        break;
      }

      case DTYPE_MULTINOMIAL: {
        const auto &print_result = [this, &text_output, variable](
            int domain_value, int domain_index) {
          text_output
              << variable.id << " " << domain_value << " "
              << (1.0 *
                  multinomial_tallies[variable.n_start_i_tally + domain_index] /
                  agg_nsamples[variable.id])
              << std::endl;
        };
        if (variable.domain_map) {  // sparse
          for (const auto &entry : *variable.domain_map)
            print_result(entry.first, entry.second);
        } else {  // dense
          for (size_t k = 0; k < variable.cardinality; ++k) print_result(k, k);
        }
        break;
      }

      default:
        abort();
    }
  }
}

}  // namespace dd
