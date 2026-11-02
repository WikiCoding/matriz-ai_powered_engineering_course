import { useData } from "../hooks/useData";
import { userDisplayPath } from "../api/users";

function Comment({ value, token }) {
  const author = useData(userDisplayPath(value.authorId), token);
  return (
    <article className="comment">
      <div>
        <strong>{author.data?.displayName || "Team member"}</strong>
        <time>{value.createdAt}</time>
      </div>
      <p>{value.content}</p>
    </article>
  );
}
export default Comment;
