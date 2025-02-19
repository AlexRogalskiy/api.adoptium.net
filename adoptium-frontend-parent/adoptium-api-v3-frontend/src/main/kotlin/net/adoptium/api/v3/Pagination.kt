package net.adoptium.api.v3

import javax.ws.rs.NotFoundException
import javax.ws.rs.core.Response
import javax.ws.rs.core.UriInfo
import kotlin.math.min

object Pagination {
    private const val defaultPageSizeNum = 10
    private const val maxPageSizeNum = 20
    const val largerPageSizeNum = 50
    const val defaultPageSize = defaultPageSizeNum.toString()
    const val maxPageSize = maxPageSizeNum.toString()
    const val largerPageSize = largerPageSizeNum.toString()

    fun <T, U> formPagedResponse(data: T, uriInfo: UriInfo, pageInfo: PaginationInfo<U>): Response {
        var builder = Response
            .ok()
            .entity(data)

        if (pageInfo.next != null) {
            val nextUri = uriInfo
                .requestUriBuilder
                .replaceQueryParam("page", pageInfo.next)
                .replaceQueryParam("page_size", pageInfo.pageSize)
                .build()

            builder = builder.link(nextUri, "next")
        }

        return builder.build()
    }

    fun <T> getResponseForPage(uriInfo: UriInfo, pageSize: Int?, page: Int?, releases: Sequence<T>, maxPageSizeNum: Int = this.maxPageSizeNum): Response {
        val pageInfo = getPage(pageSize, page, releases, maxPageSizeNum)
        return formPagedResponse(pageInfo.data, uriInfo, pageInfo)
    }

    fun <T> getPage(pageSize: Int?, page: Int?, releases: Sequence<T>, maxPageSizeNum: Int = this.maxPageSizeNum): PaginationInfo<T> {
        val pageSizeNum = min(maxPageSizeNum, (pageSize ?: defaultPageSizeNum))
        val pageNum = page ?: 0

        val chunked = releases.chunked(pageSizeNum)

        return try {
            val pages = chunked.drop(pageNum).take(2).toList()

            if (pages.isEmpty()) {
                throw NotFoundException("Page not available")
            }

            val hasNext = try {
                if (pages.size > 1) {
                    pages[1].isNotEmpty()
                } else {
                    false
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            PaginationInfo(
                if (hasNext) pageNum + 1 else null,
                pageSizeNum,
                pages[0]
            )
        } catch (e: IndexOutOfBoundsException) {
            throw NotFoundException("Page not available")
        }
    }

    data class PaginationInfo<T>(
        val next: Int?,
        val pageSize: Int,
        val data: List<T>
    )
}
