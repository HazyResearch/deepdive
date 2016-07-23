#include "inference_result.h"
#include "factor_graph.h"
#include <iostream>
#include <memory>

namespace dd {

InferenceResult::InferenceResult(const CompactFactorGraph &fg,
                                 const CmdParser &opts)
    : fg(fg),
      opts(opts),
      weight_values_normalizer(1),
      nvars(fg.size.num_variables),
      nweights(fg.size.num_weights),
      ntallies(0),
      categorical_tallies(),
      agg_means(new variable_value_t[nvars]),
      agg_nsamples(new num_samples_t[nvars]),
      assignments_free(new variable_value_t[nvars]),
      assignments_evid(new variable_value_t[nvars]),
      weight_values(new weight_value_t[nweights]),
      weights_isfixed(new bool[nweights]) {}

InferenceResult::InferenceResult(const CompactFactorGraph &fg,
                                 const Weight weights[], const CmdParser &opts)
    : InferenceResult(fg, opts) {
  for (weight_id_t t = 0; t < nweights; ++t) {
    const Weight &weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weights_isfixed[weight.id] = weight.isfixed;
  }

  ntallies = 0;
  for (variable_id_t t = 0; t < nvars; ++t) {
    const Variable &variable = fg.variables[t];
    assignments_free[variable.id] = variable.assignment_free;
    assignments_evid[variable.id] = variable.assignment_evid;
    if (variable.domain_type == DTYPE_CATEGORICAL) {
      ntallies += variable.cardinality;
    }
  }

  categorical_tallies.reset(new num_samples_t[ntallies]);

  clear_variabletally();
}

InferenceResult::InferenceResult(const InferenceResult &other)
    : InferenceResult(other.fg, other.opts) {
  COPY_ARRAY_UNIQUE_PTR_MEMBER(assignments_evid, nvars);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(agg_means, nvars);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(agg_nsamples, nvars);

  COPY_ARRAY_UNIQUE_PTR_MEMBER(weight_values, nweights);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(weights_isfixed, nweights);

  ntallies = other.ntallies;
  categorical_tallies.reset(new num_samples_t[ntallies]);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(categorical_tallies, ntallies);
}

void InferenceResult::merge_weights_from(const InferenceResult &other) {
  assert(nweights == other.nweights);
  for (weight_id_t j = 0; j < nweights; ++j)
    weight_values[j] += other.weight_values[j];
  ++weight_values_normalizer;
}

void InferenceResult::average_regularize_weights(double current_stepsize) {
  for (weight_id_t j = 0; j < nweights; ++j) {
    weight_values[j] /= weight_values_normalizer;
    if (!weights_isfixed[j]) {
      switch (opts.regularization) {
        case REG_L2: {
          weight_values[j] *= (1.0 / (1.0 + opts.reg_param * current_stepsize));
          break;
        }
        case REG_L1: {
          weight_values[j] += opts.reg_param * (weight_values[j] < 0);
          break;
        }
        default:
          std::abort();
      }
    }
  }
  weight_values_normalizer = 1;
}

void InferenceResult::copy_weights_to(InferenceResult &other) const {
  assert(nweights == other.nweights);
  for (weight_id_t j = 0; j < nweights; ++j)
    if (!weights_isfixed[j]) other.weight_values[j] = weight_values[j];
}

void InferenceResult::show_weights_snippet(std::ostream &output) const {
  output << "LEARNING SNIPPETS (QUERY WEIGHTS):" << std::endl;
  num_weights_t ct = 0;
  for (weight_id_t j = 0; j < nweights; ++j) {
    ++ct;
    output << "   " << j << " " << weight_values[j] << std::endl;
    if (ct % 10 == 0) {
      break;
    }
  }
  output << "   ..." << std::endl;
}

void InferenceResult::dump_weights_in_text(std::ostream &text_output) const {
  for (weight_id_t j = 0; j < nweights; ++j) {
    text_output << j << " " << weight_values[j] << std::endl;
  }
}

void InferenceResult::clear_variabletally() {
  for (variable_id_t i = 0; i < nvars; ++i) {
    agg_means[i] = 0.0;
    agg_nsamples[i] = 0.0;
  }
  for (num_tallies_t i = 0; i < ntallies; ++i) {
    categorical_tallies[i] = 0;
  }
}

void InferenceResult::aggregate_marginals_from(const InferenceResult &other) {
  // TODO maybe make this an operator+ after separating marginals from weights
  assert(nvars == other.nvars);
  assert(ntallies == other.ntallies);
  for (variable_id_t j = 0; j < other.nvars; ++j) {
    const Variable &variable = other.fg.variables[j];
    agg_means[variable.id] += other.agg_means[variable.id];
    agg_nsamples[variable.id] += other.agg_nsamples[variable.id];
  }
  for (num_tallies_t j = 0; j < other.ntallies; ++j) {
    categorical_tallies[j] += other.categorical_tallies[j];
  }
}

void InferenceResult::show_marginal_snippet(std::ostream &output) const {
  output << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
  num_variables_t ct = 0;
  for (variable_id_t j = 0; j < fg.size.num_variables; ++j) {
    const Variable &variable = fg.variables[j];
    if (!variable.is_evid || opts.should_sample_evidence) {
      ++ct;
      output << "   " << variable.id
             << "  NSAMPLE=" << agg_nsamples[variable.id] << std::endl;
      switch (variable.domain_type) {
        case DTYPE_BOOLEAN:
          output << "        @ 1 -> EXP="
                 << (double)agg_means[variable.id] / agg_nsamples[variable.id]
                 << std::endl;
          break;

        case DTYPE_CATEGORICAL: {
          const auto &print_snippet = [this, &output, &variable](
              variable_value_t domain_value,
              variable_value_index_t domain_index) {
            output << "        @ " << domain_value << " -> EXP="
                   << 1.0 * categorical_tallies[variable.n_start_i_tally +
                                                domain_index] /
                          agg_nsamples[variable.id]
                   << std::endl;
          };
          if (variable.domain_map) {  // sparse case
            for (const auto &entry : *variable.domain_map)
              print_snippet(entry.first, entry.second);
          } else {  // dense case, full domain implied
            for (variable_value_index_t j = 0; j < variable.cardinality; ++j)
              print_snippet(j, j);
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

void InferenceResult::show_marginal_histogram(std::ostream &output) const {
  // show a histogram of inference results
  output << "INFERENCE CALIBRATION (QUERY BINS):" << std::endl;
  std::vector<num_variables_t> abc;
  for (int i = 0; i <= 10; ++i) {
    abc.push_back(0);
  }
  num_variables_t bad = 0;
  for (variable_id_t j = 0; j < nvars; ++j) {
    const Variable &variable = fg.variables[j];
    if (!opts.should_sample_evidence && variable.is_evid) {
      continue;
    }
    int bin =
        (int)((double)agg_means[variable.id] / agg_nsamples[variable.id] * 10);
    if (bin >= 0 && bin <= 10) {
      ++abc[bin];
    } else {
      ++bad;
    }
  }
  abc[9] += abc[10];
  for (int i = 0; i < 10; ++i) {
    output << "PROB BIN 0." << i << "~0." << (i + 1) << "  -->  # " << abc[i]
           << std::endl;
  }
}

void InferenceResult::dump_marginals_in_text(std::ostream &text_output) const {
  for (variable_id_t j = 0; j < nvars; ++j) {
    const Variable &variable = fg.variables[j];
    if (variable.is_evid && !opts.should_sample_evidence) {
      continue;
    }

    switch (variable.domain_type) {
      case DTYPE_BOOLEAN: {
        text_output << variable.id << " " << 1 << " "
                    << ((double)agg_means[variable.id] /
                        agg_nsamples[variable.id])
                    << std::endl;
        break;
      }

      case DTYPE_CATEGORICAL: {
        const auto &print_result = [this, &text_output, &variable](
            variable_value_t domain_value,
            variable_value_index_t domain_index) {
          text_output
              << variable.id << " " << domain_value << " "
              << (1.0 *
                  categorical_tallies[variable.n_start_i_tally + domain_index] /
                  agg_nsamples[variable.id])
              << std::endl;
        };
        if (variable.domain_map) {  // sparse domain
          for (const auto &entry : *variable.domain_map)
            print_result(entry.first, entry.second);
        } else {  // dense, full domain implied
          for (variable_value_index_t k = 0; k < variable.cardinality; ++k)
            print_result(k, k);
        }
        break;
      }

      default:
        std::abort();
    }
  }
}

}  // namespace dd
