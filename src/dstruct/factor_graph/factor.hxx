namespace dd{

	/** Return the value of the "equality test" of the variables in the factor,
	 * with the variable of index vid (wrt the factor) is set to the value of
	 * the 'proposal' argument.
	 *
	 */
	inline double dd::CompactFactor::_potential_equal(
	  const VariableInFactor * const vifs,
	  const VariableValue * const var_values,
	  const VariableIndex & vid, const VariableValue & proposal) const{

	  const VariableInFactor & vif = vifs[n_start_i_vif];
	  /* We use the value of the first variable in the factor as the "gold" standard" */
	  const bool firstsat = (vif.vid == vid) ? vif.satisfiedUsing(proposal) :
	  	  vif.satisfiedUsing(var_values[vif.vid]);

	  /* Iterate over the factor variables */
	  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables);i_vif++){
	    const VariableInFactor & vif = vifs[i_vif];
	    const bool satisfied = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	    /* Early return as soon as we find a mismatch */
	    if(satisfied != firstsat) return 0.0;
	  }
	  return 1.0;
	}

	/** Return the value of the logical AND of the variables in the factor, with
	 * the variable of index vid (wrt the factor) is set to the value of the
	 * 'proposal' argument.
	 *
	 */
	inline double dd::CompactFactor::_potential_and(
	  const VariableInFactor * const vifs,
	  const VariableValue * const var_values,
	  const VariableIndex & vid,
	  const VariableValue & proposal) const{

	  /* Iterate over the factor variables */
	  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables);i_vif++){
	    const VariableInFactor & vif = vifs[i_vif];
	    const bool satisfied = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	    /* Early return as soon as we find a variable that is not satisfied */
	    if(!satisfied) return 0.0;
	  }
	  return 1.0;

	}

	/** Return the value of the logical OR of the variables in the factor, with
	 * the variable of index vid (wrt the factor) is set to the value of the
	 * 'proposal' argument.
	 *
	 */
	inline double dd::CompactFactor::_potential_or(
	  const VariableInFactor * const vifs,
	  const VariableValue * const var_values,
	  const VariableIndex & vid,
	  const VariableValue & proposal) const{

	  /* Iterate over the factor variables */
	  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables);i_vif++){
	    const VariableInFactor & vif = vifs[i_vif];
	    const bool satisfied = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	    /* Early return as soon as we find a variable that is satisfied */
	    if(satisfied) return 1.0;
	  }
	  return 0.0;

	}

	/** Return the value of the 'imply (MLN version)' of the variables in the
	 * factor, with the variable of index vid (wrt the factor) is set to the
	 * value of the 'proposal' argument.
	 *
	 * The head of the 'imply' rule is stored as the *last* variable in the
	 * factor.
	 *
	 * The truth table of the 'imply (MLN version)' function requires to return
	 * 0.0 if the body of the imply is satisfied but the head is not, and to
	 * return 1.0 if the body is not satisfied.
	 *
	 */
	inline double dd::CompactFactor::_potential_imply_mln(
	  const VariableInFactor * const vifs,
	  const VariableValue * const var_values,
	  const VariableIndex & vid,
	  const VariableValue & proposal) const{

	  /* Compute the value of the body of the rule */
	  bool bBody = true;
	  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables - 1); i_vif++){
	    const VariableInFactor & vif = vifs[i_vif];
	    // If it is the proposal variable, we use the truth value of the proposal
	    bBody &= (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	  }

	  if(!bBody) {
	  	// Early return if the body is not satisfied
	    return 1.0;
	  } else {
	  	// Compute the value of the head of the rule
	    const VariableInFactor & vif = vifs[n_start_i_vif + n_variables - 1]; // encoding of the head, should be more structured.
	    const bool bHead = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	    return bHead ? 1.0 : 0.0;
	  }

	}

	/** Return the value of the 'imply' of the variables in the factor, with the
	 * variable of index vid (wrt the factor) is set to the value of the
	 * 'proposal' argument.
	 *
	 * The head of the 'imply' rule is stored as the *last* variable in the
	 * factor.
	 *
	 * The truth table of the 'imply' function requires to return
	 * -1.0 if the body of the rule is satisfied but the head is not, and to
	 *  return 0.0 if the body is not satisfied.
	 *
	 */
	inline double dd::CompactFactor::_potential_imply(
	  const VariableInFactor * const vifs,
	  const VariableValue * const var_values,
	  const VariableIndex & vid,
	  const VariableValue & proposal) const{

	  /* Compute the value of the body of the rule */
	  bool bBody = true;
	  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables - 1); i_vif++){
	    const VariableInFactor & vif = vifs[i_vif];
	    // If it is the proposal variable, we use the truth value of the proposal
	    bBody &= (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	  }

	  if(!bBody) {
	  	// Early return if the body is not satisfied
	    return 0.0;
	  } else {
	  	// Compute the value of the head of the rule */
	    const VariableInFactor & vif = vifs[n_start_i_vif + n_variables - 1]; // encoding of the head, should be more structured.
	    bool bHead = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
	    return bHead ? 1.0 : -1.0;
	  }

	}

	// potential for multinomial variable
	inline double dd::CompactFactor::_potential_multinomial(const VariableInFactor * vifs,
	  const VariableValue * var_values, const VariableIndex & vid, const VariableValue & proposal) const {

	  return 1.0;
	}

}
