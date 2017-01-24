#include "inference_result.h"
#include "factor_graph.h"
#include <cstring>
#include <iostream>
#include <iomanip>
#include <memory>

namespace dd {

InferenceResult::InferenceResult(const FactorGraph &fg, const CmdParser &opts)
    : fg(fg),
      opts(opts),
      nvars(fg.size.num_variables),
      nweights(fg.size.num_weights),
      ntallies(fg.size.num_values),
      sample_tallies(new size_t[fg.size.num_values]),
      agg_nsamples(new size_t[nvars]),
      assignments_free(new size_t[nvars]),
      assignments_evid(new size_t[nvars]),
      weight_values(new double[nweights]),
      weight_grads(new float[nweights]),
      weights_isfixed(new bool[nweights]) {}

InferenceResult::InferenceResult(const FactorGraph &fg, const Weight weights[],
                                 const CmdParser &opts)
    : InferenceResult(fg, opts) {
  for (size_t t = 0; t < nweights; ++t) {
    const Weight &weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weight_grads[weight.id] = 0;
    weights_isfixed[weight.id] = weight.isfixed;
  }

  for (size_t t = 0; t < nvars; ++t) {
    const Variable &variable = fg.variables[t];
    assignments_free[variable.id] =
        variable.is_evid ? variable.assignment_dense : 0;
    assignments_evid[variable.id] = assignments_free[variable.id];
  }

  clear_variabletally();
}

InferenceResult::InferenceResult(const InferenceResult &other)
    : InferenceResult(other.fg, other.opts) {
  COPY_ARRAY_UNIQUE_PTR_MEMBER(assignments_evid, nvars);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(agg_nsamples, nvars);

  COPY_ARRAY_UNIQUE_PTR_MEMBER(weight_values, nweights);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(weight_grads, nweights);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(weights_isfixed, nweights);

  ntallies = other.ntallies;
  COPY_ARRAY_UNIQUE_PTR_MEMBER(sample_tallies, ntallies);
}

void InferenceResult::merge_gradients_from(const InferenceResult &other) {
  assert(nweights == other.nweights);
  for (size_t j = 0; j < nweights; ++j) {
    weight_grads[j] += other.weight_grads[j];
  }
}

void InferenceResult::reset_gradients() {
  std::memset(weight_grads.get(), 0, nweights * sizeof(float));
}

void InferenceResult::merge_weights_from(const InferenceResult &other) {
  assert(nweights == other.nweights);
  for (size_t j = 0; j < nweights; ++j) {
    weight_values[j] += other.weight_values[j];
  }
}

void InferenceResult::average_weights(size_t count) {
  for (size_t j = 0; j < nweights; ++j) {
    weight_values[j] /= count;
  }
}

void InferenceResult::copy_weights_to(InferenceResult &other) const {
  assert(nweights == other.nweights);
  for (size_t j = 0; j < nweights; ++j) {
    if (!weights_isfixed[j]) other.weight_values[j] = weight_values[j];
  }
}

void InferenceResult::show_weights_snippet(std::ostream &output) const {
  output << "LEARNING SNIPPETS (QUERY WEIGHTS):" << std::endl;
  size_t ct = 0;
  for (size_t j = 0; j < nweights; ++j) {
    ++ct;
    output << "   " << j << " " << weight_values[j] << std::endl;
    if (ct % 10 == 0) {
      break;
    }
  }
  output << "   ..." << std::endl;
}

void InferenceResult::dump_weights_in_text(std::ostream &text_output) const {
  for (size_t j = 0; j < nweights; ++j) {
    text_output << j << " " << weight_values[j] << std::endl;
  }
}

void InferenceResult::clear_variabletally() {
  for (size_t i = 0; i < nvars; ++i) {
    agg_nsamples[i] = 0;
  }
  for (size_t i = 0; i < ntallies; ++i) {
    sample_tallies[i] = 0;
  }
}

void InferenceResult::aggregate_marginals_from(const InferenceResult &other) {
  // TODO maybe make this an operator+ after separating marginals from weights
  assert(nvars == other.nvars);
  assert(ntallies == other.ntallies);
  for (size_t j = 0; j < other.nvars; ++j) {
    const Variable &variable = other.fg.variables[j];
    agg_nsamples[variable.id] += other.agg_nsamples[variable.id];
  }
  for (size_t j = 0; j < other.ntallies; ++j) {
    sample_tallies[j] += other.sample_tallies[j];
  }
}

void InferenceResult::show_marginal_snippet(std::ostream &output) const {
  output << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
  size_t ct = 0;
  for (size_t j = 0; j < fg.size.num_variables; ++j) {
    const Variable &variable = fg.variables[j];
    if (!variable.is_evid || opts.should_sample_evidence) {
      ++ct;
      output << "   " << variable.id
             << "  NSAMPLE=" << agg_nsamples[variable.id] << std::endl;

      const auto &print_snippet = [this, &output, &variable](
          size_t domain_value, size_t domain_index) {
        output << "      @ " << domain_value << " -> EXP="
               << 1.0 * sample_tallies[variable.var_val_base + domain_index] /
                      agg_nsamples[variable.id]
               << std::endl;
      };

      switch (variable.domain_type) {
        case DTYPE_BOOLEAN:
          print_snippet(1, 0);
          break;

        case DTYPE_CATEGORICAL: {
          for (size_t j = 0; j < variable.cardinality; ++j) {
            print_snippet(fg.get_var_value_at(variable, j), j);
          }
          break;
        }

        default:
          std::abort();
      }

      if (ct % 10 == 0) {
        break;
      }
    }
  }
  output << "   ..." << std::endl;
}

void InferenceResult::show_marginal_histogram(std::ostream &output,
                                              const size_t bins) const {
  // show a histogram of inference results
  output << "INFERENCE CALIBRATION (QUERY BINS):" << std::endl;
  std::vector<size_t> abc(bins + 1, 0);

  size_t bad = 0;
  for (size_t j = 0; j < nvars; ++j) {
    const Variable &variable = fg.variables[j];
    if (!opts.should_sample_evidence && variable.is_evid) {
      continue;
    }
    for (size_t k = 0; k < variable.internal_cardinality(); ++k) {
      size_t bin = (size_t)((double)sample_tallies[variable.var_val_base + k] /
                            agg_nsamples[variable.id] * bins);
      if (bin <= bins) {
        ++abc[bin];
      } else {
        ++bad;
      }
    }
  }
  abc[bins - 1] += abc[bins];

  const std::ios::fmtflags flags(output.flags());  // save ostream settings
  output << std::fixed;  // specify number of decimals (rather than sig figs)

  // save precision and set new one
  const size_t prec = output.precision(std::max((int)ceil(log10(bins)), 1));

  for (size_t i = 0; i < bins; ++i) {
    output << "PROB BIN " << (float)i / bins << "~" << (float)(i + 1) / bins
           << "  -->  # " << abc[i] << std::endl;
  }

  // restore ostream settings
  output.flags(flags);
  output << std::setprecision(prec);
}

void InferenceResult::dump_marginals_in_text(std::ostream &text_output) const {
  for (size_t j = 0; j < nvars; ++j) {
    const Variable &variable = fg.variables[j];
    if (variable.is_evid && !opts.should_sample_evidence) {
      continue;
    }

    const auto &print_result = [this, &text_output, &variable](
        size_t domain_value, size_t domain_index) {
      text_output << variable.id << " " << domain_value << " "
                  << 1.0 *
                         sample_tallies[variable.var_val_base + domain_index] /
                         agg_nsamples[variable.id]
                  << std::endl;
    };

    switch (variable.domain_type) {
      case DTYPE_BOOLEAN:
        print_result(1, 0);
        break;

      case DTYPE_CATEGORICAL: {
        for (size_t j = 0; j < variable.cardinality; ++j) {
          print_result(fg.get_var_value_at(variable, j), j);
        }
        break;
      }

      default:
        std::abort();
    }
  }
}

}  // namespace dd
