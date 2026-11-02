function Pager({ data, page, change }) {
  return data?.totalPages > 1 ? (
    <div className="pager">
      <button disabled={!page} onClick={() => change(page - 1)}>
        Previous
      </button>
      <span>
        Page {page + 1} of {data.totalPages}
      </span>
      <button
        disabled={page + 1 >= data.totalPages}
        onClick={() => change(page + 1)}
      >
        Next
      </button>
    </div>
  ) : null;
}
export default Pager;
