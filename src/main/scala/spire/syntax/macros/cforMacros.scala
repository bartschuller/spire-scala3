package spire.syntax.macros

import language.implicitConversions

import quoted._
import quoted.autolift._
import quoted.matching._
import tasty.Reflection

import collection.immutable.NumericRange

type RangeLike = Range | NumericRange[Long]
type RangeElem[X] = X match {
  case Range              => Int
  case NumericRange[Long] => Long
}

inline def cforInline[A](init: => A, test: => A => Boolean, next: => A => A, body: => A => Unit): Unit = {
  var index = init
  while (test(index)) {
    body(index)
    index = next(index)
  }
}

inline def tag[A: Type] given (ref: Reflection): ref.Type = {
  import ref._
  the[quoted.Type[A]].unseal.tpe
}

def cforRangeMacroGen[A <: RangeLike : Type](r: Expr[A], body: Expr[RangeElem[A] => Unit])
    given (ref: Reflection): Expr[Unit] = {
  import ref._

  tag[A] match {
    case t if t <:< tag[Range]              => cforRangeMacro(r.cast[Range], body.cast[Int => Unit])
    case t if t <:< tag[NumericRange[Long]] => cforRangeMacroLong(r.cast[NumericRange[Long]], body.cast[Long => Unit])
    case t                                  => QuoteError(s"Uneligable Range type ${t.show}", r)
  }
}

def cforRangeMacroLong(r: Expr[NumericRange[Long]], body: Expr[Long => Unit]) given Reflection : Expr[Unit] = {

  def strideUpUntil(fromExpr: Expr[Long], untilExpr: Expr[Long], stride: Expr[Long]): Expr[Unit] = '{
    var index = $fromExpr
    val limit = $untilExpr
    while (index < limit) {
      ${ body('index) }
      index += $stride
    }
  }

  def strideUpTo(fromExpr: Expr[Long], untilExpr: Expr[Long], stride: Expr[Long]): Expr[Unit] = '{
    var index = $fromExpr
    val end   = $untilExpr
    while (index <= end) {
      ${ body('index) }
      index += $stride
    }
  }

  def strideDownTo(fromExpr: Expr[Long], untilExpr: Expr[Long], stride: Expr[Long]): Expr[Unit] = '{
    var index = $fromExpr
    val end   = $untilExpr
    while (index >= end) {
      ${ body('index) }
      index -= $stride
    }
  }

  def strideDownUntil(fromExpr: Expr[Long], untilExpr: Expr[Long], stride: Expr[Long]): Expr[Unit] = '{
    var index = $fromExpr
    val limit = $untilExpr
    while (index > limit) {
      ${ body('index) }
      index -= $stride
    }
  }

  r match {
    case '{ ($i: Long) until $j } => strideUpUntil(i,j,1)
    case '{ ($i: Long) to $j }    => strideUpTo(i,j,1)

    case '{ ($i: Long) until $j by $step } =>
      step match {
        case Const(k) if k > 0  => strideUpUntil(i,j,k)
        case Const(k) if k < 0  => strideDownUntil(i,j,-k)
        case Const(k) if k == 0 => QuoteError("zero stride", step)
        case _                  => '{ val b = $body; $r.foreach(b) }
      }

    case '{ ($i: Long) to $j by $step } =>
      step match {
        case Const(k) if k > 0  => strideUpTo(i,j,k)
        case Const(k) if k < 0  => strideDownTo(i,j,-k)
        case Const(k) if k == 0 => QuoteError("zero stride", step)
        case _                  => '{ val b = $body; $r.foreach(b) }
      }

    case _ => '{ val b = $body; $r.foreach(b) }
  }
}

def cforRangeMacro(r: Expr[Range], body: Expr[Int => Unit]) given Reflection : Expr[Unit] = {
  
  def strideUpUntil(fromExpr: Expr[Int], untilExpr: Expr[Int], stride: Expr[Int]): Expr[Unit] = '{
    var index = $fromExpr
    val limit = $untilExpr
    while (index < limit) {
      ${ body('index) }
      index += $stride
    }
  }

  def strideUpTo(fromExpr: Expr[Int], untilExpr: Expr[Int], stride: Expr[Int]): Expr[Unit] = '{
    var index = $fromExpr
    val end   = $untilExpr
    while (index <= end) {
      ${ body('index) }
      index += $stride
    }
  }

  def strideDownTo(fromExpr: Expr[Int], untilExpr: Expr[Int], stride: Expr[Int]): Expr[Unit] = '{
    var index = $fromExpr
    val end   = $untilExpr
    while (index >= end) {
      ${ body('index) }
      index -= $stride
    }
  }

  def strideDownUntil(fromExpr: Expr[Int], untilExpr: Expr[Int], stride: Expr[Int]): Expr[Unit] = '{
    var index = $fromExpr
    val limit = $untilExpr
    while (index > limit) {
      ${ body('index) }
      index -= $stride
    }
  }
  
  r match {
    case '{ ($i: Int) until $j } => strideUpUntil(i,j,1)
    case '{ ($i: Int) to $j }    => strideUpTo(i,j,1)

    case '{ ($i: Int) until $j by $step } =>
      step match {
        case Const(k) if k > 0  => strideUpUntil(i,j,k)
        case Const(k) if k < 0  => strideDownUntil(i,j,-k)
        case Const(k) if k == 0 => QuoteError("zero stride", step)
        case _                  => '{ val b = $body; $r.foreach(b) }
      }

    case '{ ($i: Int) to $j by $step } =>
      step match {
        case Const(k) if k > 0  => strideUpTo(i,j,k)
        case Const(k) if k < 0  => strideDownTo(i,j,-k)
        case Const(k) if k == 0 => QuoteError("zero stride", step)
        case _                  => '{ val b = $body; $r.foreach(b) }
      }

    case _ => '{ val b = $body; $r.foreach(b) }
  }
}