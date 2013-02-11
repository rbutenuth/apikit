/**
 * Mule Rest Module
 *
 * Copyright 2011-2012 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * This software is protected under international copyright law. All use of this software is
 * subject to MuleSoft's Master Subscription Agreement (or other master license agreement)
 * separately entered into in writing between you and MuleSoft. If such an agreement is not
 * in place, you may not use the software.
 */

package org.mule.module.wsapi.rest.uri;

import java.util.List;


/**
 * Defines tokens which use an operator to handle one or more variables.  
 * 
 * @author Christophe Lauret
 * @version 9 February 2009
 */
public interface TokenOperator extends Token
{

  /**
   * Returns the list of variables used in this token.
   * 
   * @return the list of variables.
   */
  List<Variable> variables();

}