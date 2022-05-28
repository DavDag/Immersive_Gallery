package it.unipi.di.sam.immersivegallery.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

// Create a type alias to improve readability
private typealias InflateFun<T> = (LayoutInflater, ViewGroup?, Boolean) -> T

/**
 * [BaseFragment] class that wraps common setup for [Fragment].
 *
 * When extending this class, "T" must be a [ViewBinding] generated class and "inflate" a ref to
 * its inflate function.
 * Kotlin's oop design does not allow static function calls for templated types.
 *
 * ex.
 * ```
 * class MyFragment: BaseFragment<MyFragmentBinding>(MyFragmentBinding::inflate) {
 *
 *      override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
 *          super.onViewCreated(view, savedInstanceState)
 *
 *          binding.textview.text = "Can access bindings !"
 *      }
 * }
 * ```
 *
 */
abstract class BaseFragment<T : ViewBinding>(
    private val inflate: InflateFun<T>
) : Fragment() {

    // Ref to bindings
    // Its valid ONLY BETWEEN onCreateView and onDestroyView
    private var _binding: T? = null

    // Getter to retrieve bindings (as non-null)
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // By using the "inflate" reference, we instantiate a "T" bindings class
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    // Called when the view is created (and the binding is available) to setup the fragment
    abstract fun setup(savedInstanceState: Bundle?)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Forward UI creation
        this.setup(savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Ensure binding is only valid between onCreate and onDestroy
        _binding = null
    }
}